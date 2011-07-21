# Makefile to output the User Guide in various formats
#
# Dependencies: you need to install pandoc and pdflatex
# On Debian/Ubuntu: sudo apt-get install pandoc tetex-bin
# On windows, rely on the cygwin tools distribution

.DEFAULT_GOAL: all

all: html pdf odt

html: User-Guide.pdc
	pandoc -s -S --toc -c lsc-user-guide.css -o LSC-User-Guide.html User-Guide.pdc
	
pdf: User-Guide.pdc
	markdown2pdf  --toc -o LSC-User-Guide.pdf User-Guide.pdc

#odt: User-Guide.pdc
#	pandoc -s -S --toc -o LSC-User-Guide.odt User-Guide.pdc

clean:
	rm -f LSC-User-Guide.odt
	rm -f LSC-User-Guide.pdf*
	rm -f LSC-User-Guide.html

.PHONY: all html pdf odt clean
