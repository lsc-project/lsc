function compute_all_filter()
{
    var filter = "(&(uid=*)(createTimeStamp>=" +
                 (new Date().getFullYear()-1) +
                 "1231235959Z))";
    return filter;
}

function compute_one_filter()
{
    var filter = "(&(uid=" +
                 pivotAttributes['uid'] +
                 ")(createTimeStamp>=" +
                 (new Date().getFullYear()-1) +
                 "1231235959Z))";
    return filter;
}
