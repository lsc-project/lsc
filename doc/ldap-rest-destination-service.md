# LDAP REST destination service

`LdapRestDstService` is an `IWritableService` implementation that
sends modifications through an HTTP/JSON API instead of writing
directly to an LDAP directory. It targets the
[ldap-rest](https://github.com/linagora/ldap-rest) project, which
exposes an LDAP-compatible REST API on top of an LDAP back-end and
adds fine-grained ACLs, schema validation, audit logging and
downstream webhooks.

## When to use it

Use `<ldapRestDestinationService>` when you want LSC to feed
identity changes into a system that:

- enforces ACLs on a per-resource / per-attribute basis;
- runs business logic (provisioning, e-mail, badge…) on each
  change via webhooks;
- needs a strict audit trail tied to a service identity;
- speaks JSON rather than LDAP for operational reasons.

When the destination is a plain LDAP directory the existing
`<ldapDestinationService>` is more efficient (single TCP
connection, no JSON round-trip).

## Configuration

Drop a `<ldapRestDestinationService>` element in your task. Fields:

| element             | required | default  | meaning                                                                  |
|---------------------|----------|----------|--------------------------------------------------------------------------|
| `<name>`            | yes      | —        | task-unique name, inherited from `serviceType`                           |
| `<connection>`      | yes      | —        | inherited from `serviceType`; **ignored** at runtime by this service     |
| `<baseUrl>`         | yes      | —        | scheme+host+port of the ldap-rest server, e.g. `https://ldap-rest.example.org` |
| `<resourceType>`    | yes      | —        | one of `users`, `groups`, `organizations`, or any flat resource your ldap-rest server exposes |
| `<apiPrefix>`       | no       | `/api`   | path prefix of the API (e.g. `/api/v2`); use `/` for no prefix           |
| `<auth>`            | yes      | —        | authentication block, see below                                          |
| `<timeoutMs>`       | no       | `10000`  | per-request timeout in milliseconds                                      |
| `<retries>`         | no       | `3`      | number of retries on HTTP 5xx or I/O errors                              |
| `<writeDatasetIds>` | no       | empty    | dataset ids this service is allowed to write (`<string>` children)       |

The schema requires `<connection>` because every service inherits
it from `serviceType`. `LdapRestDstService` does not actually use
the bound connection; operators usually point it to a stub LDAP
connection or any existing one to keep the schema valid.

### Authentication

Two auth strategies are supported. Pick one inside `<auth>`:

```xml
<auth>
  <bearer>${LDAP_REST_TOKEN}</bearer>
</auth>
```

or

```xml
<auth>
  <hmacServiceId>lsc-prod</hmacServiceId>
  <hmacSecret>${LDAP_REST_HMAC_SECRET}</hmacSecret>
</auth>
```

The HMAC scheme uses HMAC-SHA256 over the canonical signing string
`METHOD|PATH|TIMESTAMP|sha256(body)` and matches the verification
performed by ldap-rest server-side. Variable expansion (`${VAR}`)
follows the standard LSC configuration mechanism.

## Example

```xml
<task>
  <name>users-task</name>
  <bean>org.lsc.beans.SimpleBean</bean>

  <ldapSourceService>
    <name>src-users</name>
    <connection reference="src-ldap"/>
    <baseDn>ou=users,dc=source,dc=example,dc=com</baseDn>
    <pivotAttributes><string>uid</string></pivotAttributes>
    <fetchedAttributes>
      <string>uid</string><string>cn</string><string>sn</string>
      <string>givenName</string><string>mail</string>
    </fetchedAttributes>
    <getAllFilter><![CDATA[(objectClass=inetOrgPerson)]]></getAllFilter>
    <getOneFilter><![CDATA[(&(objectClass=inetOrgPerson)(uid={uid}))]]></getOneFilter>
    <cleanFilter><![CDATA[(&(objectClass=inetOrgPerson)(uid={uid}))]]></cleanFilter>
  </ldapSourceService>

  <ldapRestDestinationService>
    <name>dst-ldap-rest</name>
    <connection reference="dst-stub"/>
    <baseUrl>https://ldap-rest.example.org</baseUrl>
    <resourceType>users</resourceType>
    <auth>
      <bearer>${LDAP_REST_TOKEN}</bearer>
    </auth>
    <timeoutMs>10000</timeoutMs>
    <retries>3</retries>
  </ldapRestDestinationService>

  <propertiesBasedSyncOptions>
    <mainIdentifier>"uid=" + srcBean.getDatasetFirstValueById("uid") + ",ou=users,dc=target,dc=example,dc=com"</mainIdentifier>
    <defaultDelimiter>;</defaultDelimiter>
    <defaultPolicy>FORCE</defaultPolicy>
    <!-- … datasets … -->
  </propertiesBasedSyncOptions>
</task>
```

## Supported operations

| LSC operation     | HTTP call                                                           |
|-------------------|----------------------------------------------------------------------|
| `CREATE_OBJECT`   | `POST /api/v1/ldap/{resourceType}` with full attribute object       |
| `UPDATE_OBJECT`   | `PUT  /api/v1/ldap/{resourceType}/{id}` with `add`/`replace`/`delete` buckets |
| `DELETE_OBJECT`   | `DELETE /api/v1/ldap/{resourceType}/{id}` (404 → idempotent success) |
| `CHANGE_ID` (rename) | `POST /api/v1/ldap/groups/{cn}/rename` (groups, same parent)     |
| `CHANGE_ID` (move)   | `POST /api/v1/ldap/{resourceType}/{id}/move`                     |

For `groups`, mutations of the `member` attribute are routed to the
dedicated `/members` and `/members/{member}` endpoints because
ldap-rest forbids touching `member` through bulk PUT. Attribute-wide
DELETE on `member` triggers a GET-then-delete-each reconciliation.

## Identifier mapping

| family         | URL identifier                                                |
|----------------|---------------------------------------------------------------|
| `users` (flat) | RDN value, e.g. `uid=alice,…` → `alice`                       |
| `groups`       | `cn` value, e.g. `cn=admins,…` → `admins`                     |
| `organizations`| URL-encoded full DN, e.g. `ou=sales,dc=ex,dc=org` → `ou%3Dsales%2Cdc%3Dex%2Cdc%3Dorg` |

DN escapes (`\,`, `\\`, `\=`, `\xx` hex) are decoded via
`javax.naming.ldap.LdapName` so the wire identifier is the
*logical* value. The ldap-rest server re-escapes it on its side.

## Limitations

- **Binary attributes are not supported.** Values that fail strict
  UTF-8 decoding raise `LscServiceException`. Plain UTF-8 byte
  arrays (e.g. `userPassword` source) are accepted and forwarded as
  strings.
- **No bulk operations.** Each LSC modification produces one HTTP
  call (with the exception of group member reconciliation, which
  fans out one DELETE per current member).
- **REPLACE on group `member` is best-effort.** It is implemented
  as add-all-new without computing a delete-all-missing. Use
  explicit ADD/DELETE for deterministic group sync.
- **No source side.** The service only implements writes; LSC reads
  source records from a separate `<*SourceService>`.
