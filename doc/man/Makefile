# Makefile to output the User Guide in various formats
#
# Dependencies: you need to install pandoc and pdflatex
# On Debian/Ubuntu: sudo apt-get install pandoc tetex-bin
# On windows, rely on the cygwin tools distribution

.DEFAULT_GOAL: all

all: html man

html: html/lsc.1.html html/lsc-agent.1.html html/lsc.properties.5.html html/lsc.xml.5.html

man: man1/lsc.1 man1/lsc-agent.1 man5/lsc.properties.5 man5/lsc.xml.5

html/%.html : src/%.xml
	xsltproc /usr/share/xmltoman/xmltoman.xsl $< > $@
	cp /usr/share/xmltoman/xmltoman.css `dirname $@`

man5/% : src/%.xml
	xmltoman $< > $@
	
man1/% : src/%.xml
	xmltoman $< > $@
	
clean:
	rm -f man1/*.1
	rm -f man5/*.5
	rm -f html/lsc.*.html

.PHONY: all man html clean
