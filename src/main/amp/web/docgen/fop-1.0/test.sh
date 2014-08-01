#!/bin/bash
./fop -xml "/Users/dlam/Desktop/docsview/docgen.xml" -xsl ../xsl/fo/mgss.xsl -pdf testmgss.pdf -param section.label.includes.component.label 1 -param jpl.footer "The technical data in this document is controlled under the U.S. export Regulartions, release to foreign person s amy require an export authorization." -param body.start.indent 0pt -param fop1.extensions 1 -param section.autolabel 1

