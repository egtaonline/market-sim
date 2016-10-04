TEX_FILES := $(wildcard *.tex)
PDF_FILES := $(addprefix ../,$(notdir $(TEX_FILES:.tex=.pdf)))

all: $(PDF_FILES)

../%.pdf: %.tex
	rm -f $(<:.tex=).{aux,bbl,blg,log,out}
	pdflatex -draftmode -halt-on-error $< || (echo "Compilation of "$<" failed"; exit 1)
	echo ">> "$@
	export openout_any='r' && bibtex $(<:.tex=).aux || echo "No bibliography found"
	pdflatex -draftmode -halt-on-error $<
	pdflatex -halt-on-error -output-directory .. $< 
