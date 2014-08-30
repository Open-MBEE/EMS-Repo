<?xml version="1.0" encoding="UTF-8"?>
<!--Version 4.0
    Updated by: Charles E Galey (313B) 5/2/13, 
    Original OpsRev version by: Doris T Lamb (393A)-->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:d="http://docbook.org/ns/docbook" xmlns:fo="http://www.w3.org/1999/XSL/Format" version="1.0">
    <xsl:import href="profile-docbook.xsl"/>
    <!-- Apply XHLTHL extension. -->
    <xsl:import href="highlight.xsl"/>
    <xsl:import href="../oxygen_custom.xsl"/>
    
    
    <xsl:param name="hard.pagebreak" select="false"/>
    <xsl:template match="processing-instruction('hard-pagebreak')">
        <xsl:if test="$hard.pagebreak='true'">
            <fo:block break-after="page"/>
        </xsl:if>
    </xsl:template>
    
    <!-- param customizations, ulink.show suppresses showing url after links -->
    <xsl:param name="ulink.show" select="0"/>
    
    <xsl:variable name="jpl.version" select="d:book/d:info/d:releaseinfo"/>
    <xsl:variable name="jpl.date" select="d:book/d:info/d:pubdate"/>
    <xsl:variable name="jpl.docid" select="d:book/d:info/d:productnumber"/>
    <xsl:variable name="jpl.hdrtitle" select="d:book/d:info/d:titleabbrev"/>
    <xsl:variable name="jpl.prjname" select="d:book/d:info/d:publisher/d:publishername"/>

    <xsl:param name="jpl.header" select="''"/>
    <xsl:variable name="jpl.footer" select="d:book/d:info/d:legalnotice/d:para"/>
    <xsl:param name="jpl.subheader" select="''"/>
    <xsl:param name="jpl.subfooter" select="''"/>
    
    <xsl:param name="toc.section.depth" select="8"/>
    <xsl:param name="section.label.includes.component.label" select="1"/>
    <xsl:param name="section.autolabel" select="1"/>
    <xsl:param name="body.start.indent" select="1"/>
    
    <xsl:param name="header.column.widths">49 1 50</xsl:param>
    <xsl:param name="footer.column.widths">1 20 1</xsl:param>
        
    <!-- Change this section to add a different (non JPL) title page logo -->
    <xsl:param name="JPL.logo.size">36px</xsl:param>
    <xsl:param name="JPL.logo.image">http://sec274.jpl.nasa.gov/img/logos/jpl_logo(220x67).gif</xsl:param>
    <xsl:param name="JPL.logo.text1">Jet Propulsion Laboratory</xsl:param>
    <xsl:param name="JPL.logo.text2">California Institute of Technology</xsl:param> 
    
    <xsl:template name="user.pagemasters">
        <xsl:param name="element"/>
        <xsl:param name="pageclass"/>
        <xsl:param name="default-pagemaster"/>
        
        <fo:page-sequence-master master-name="jpl.title"> 
            <fo:repeatable-page-master-alternatives>
                <fo:conditional-page-master-reference 
                    master-reference="blank"
                    blank-or-not-blank="blank"/>
                <fo:conditional-page-master-reference 
                    master-reference="jpl.title-first"
                    page-position="first"/>
                <fo:conditional-page-master-reference 
                    master-reference="jpl.title-odd"
                    odd-or-even="odd"/>
                <fo:conditional-page-master-reference 
                    master-reference="jpl.title-even"
                    odd-or-even="even"/>
            </fo:repeatable-page-master-alternatives>
        </fo:page-sequence-master>
        
        <fo:simple-page-master master-name="jpl.title-first"
            page-width="{$page.width}"
            page-height="{$page.height}"
            margin-top="{$page.margin.top}"
            margin-bottom="{$page.margin.bottom}"
            margin-left="{$page.margin.inner}"
            margin-right="{$page.margin.outer}">
            <fo:region-body margin-bottom="{$body.margin.bottom}"
                margin-top="{$body.margin.top}"
                column-count="{$column.count.titlepage}">
            </fo:region-body>
            <fo:region-before region-name="xsl-region-before-first"
                extent="{$region.before.extent}"
                display-align="before"/>
            <fo:region-after region-name="xsl-region-after-first"
                extent="1in"
                display-align="before"/>
        </fo:simple-page-master>
        
        <fo:simple-page-master master-name="jpl.title-even"
            page-width="{$page.width}"
            page-height="{$page.height}"
            margin-top="{$page.margin.top}"
            margin-bottom="{$page.margin.bottom}"
            margin-left="{$page.margin.inner}"
            margin-right="{$page.margin.outer}">
            <fo:region-body margin-bottom="{$body.margin.bottom}"
                margin-top="{$body.margin.top}"
                column-count="{$column.count.titlepage}">
            </fo:region-body>
            <fo:region-before region-name="xsl-region-before-even"
                extent="{$region.before.extent}"
                display-align="before"/>
            <fo:region-after region-name="xsl-region-after-even"
                extent="1in"
                display-align="before"/>
        </fo:simple-page-master>
        
        <fo:simple-page-master master-name="jpl.title-odd"
            page-width="{$page.width}"
            page-height="{$page.height}"
            margin-top="{$page.margin.top}"
            margin-bottom="{$page.margin.bottom}"
            margin-left="{$page.margin.inner}"
            margin-right="{$page.margin.outer}">
            <fo:region-body margin-bottom="{$body.margin.bottom}"
                margin-top="{$body.margin.top}"
                column-count="{$column.count.titlepage}">
            </fo:region-body>
            <fo:region-before region-name="xsl-region-before-odd"
                extent="{$region.before.extent}"
                display-align="before"/>
            <fo:region-after region-name="xsl-region-after-odd"
                extent="1in"
                display-align="before"/>
        </fo:simple-page-master>
    </xsl:template>
    
    <xsl:template name="select.user.pagemaster">
        <xsl:param name="element"/>
        <xsl:param name="pageclass"/>
        <xsl:param name="default-pagemaster"/>
        
        <!-- Return my customized title page master name if for titlepage,
       otherwise return the default -->
        
        <xsl:choose>
            <xsl:when test="$default-pagemaster = 'titlepage'">
                <xsl:value-of select="'jpl.title'" />
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$default-pagemaster"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    <!-- header is smaller -->
    <xsl:template name="header.content">
        <xsl:param name="pageclass" select="''"/>
        <xsl:param name="sequence" select="''"/>
        <xsl:param name="position" select="''"/>
        <xsl:param name="gentext-key" select="''"/>

        <!-- sequence can be odd, even, first, blank -->
        <!-- position can be left, center, right -->
        <xsl:choose>
            <xsl:when test="$position='left' and $pageclass != 'jpl.title'">
                <fo:block>
                    <xsl:value-of select="$jpl.version"/>
                </fo:block>
                <fo:block>
                    <xsl:value-of select="$jpl.date"/>
                </fo:block>
            </xsl:when>
            <xsl:when test="$position='left' and $pageclass = 'jpl.title' and $sequence !='first'">
                <fo:block>
                    <xsl:value-of select="$jpl.version"/>
                </fo:block>
                <fo:block>
                    <xsl:value-of select="$jpl.date"/>
                </fo:block>
            </xsl:when>
            <xsl:when test="$position='right' and $pageclass != 'jpl.title'">
                <fo:block>
                    <xsl:value-of select="$jpl.docid"/>
                </fo:block>
                <fo:block>
                    <xsl:value-of select="$jpl.prjname"/>
                    <xsl:call-template name="gentext.space"/>
                    <xsl:value-of select="$jpl.hdrtitle"/>
                </fo:block>
            </xsl:when>
            <xsl:when test="$position='right' and $pageclass = 'jpl.title' and $sequence !='first'">
                <fo:block>
                    <xsl:value-of select="$jpl.docid"/>
                </fo:block>
                <fo:block>
                    <xsl:value-of select="$jpl.prjname"/>
                    <xsl:call-template name="gentext.space"/>
                    <xsl:value-of select="$jpl.hdrtitle"/>
                </fo:block>
            </xsl:when>
            <xsl:when test="$pageclass != 'jpl.title' and $position='center'">
                <xsl:if test="$jpl.header!=''">
                    <fo:block><xsl:value-of select="$jpl.header"/></fo:block>
                </xsl:if>
                <xsl:if test="$jpl.subheader">
                    <fo:block><xsl:value-of select="$jpl.subheader"/></fo:block>
                </xsl:if>
            </xsl:when>
            <xsl:when test="$pageclass = 'jpl.title' and $position='center' and $sequence !='first'">
                <xsl:if test="$jpl.header!=''">
                    <fo:block><xsl:value-of select="$jpl.header"/></fo:block>
                </xsl:if>
                <xsl:if test="$jpl.subheader!=''">
                    <fo:block><xsl:value-of select="$jpl.subheader"/></fo:block>
                </xsl:if>
            </xsl:when>
            <xsl:otherwise>
                <fo:block/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

        <!-- footer is italics, smaller -->
        <xsl:template name="footer.content">
            <xsl:param name="pageclass" select="''"/>
            <xsl:param name="sequence" select="''"/>
            <xsl:param name="position" select="''"/>
            <xsl:param name="gentext-key" select="''"/>

            <!-- pageclass can be front, body, back -->
            <!-- sequence can be odd, even, first, blank -->
            <!-- position can be left, center, right -->
            <xsl:choose>
            	<xsl:when test="$position='center' and $pageclass != 'jpl.title'">
                    <fo:block><fo:page-number/></fo:block>
                    <fo:block font-size="7pt" font-style="italic"><xsl:value-of select="$jpl.subfooter"/></fo:block>
                    <fo:block font-size="9pt" font-style="italic"><xsl:value-of select="$jpl.footer"/></fo:block>
            	</xsl:when>
                <xsl:when test="$pageclass = 'jpl.title' and $sequence = 'first' and not(d:info/d:cover)">
                    <fo:block>
                        <fo:external-graphic content-height="{$JPL.logo.size}">
                            <xsl:attribute name="src">
                                <xsl:call-template name="fo-external-image">
                                    <xsl:with-param name="filename" select="$JPL.logo.image"/>
                                </xsl:call-template>   
                            </xsl:attribute>
                        </fo:external-graphic>
                    </fo:block>
                    <fo:block text-align="left" color="gray" ><xsl:value-of select="$JPL.logo.text1"/></fo:block> 
                    <fo:block font-size="10pt"  text-align="left" font-style="italic" color="gray" space-after="0.5in"><xsl:value-of select="$JPL.logo.text2"/></fo:block> 
                </xsl:when>
                <xsl:when test="$pageclass = 'jpl.title' and $sequence != 'first'">
                    <fo:block>
                        <fo:external-graphic content-height="{$JPL.logo.size}">
                            <xsl:attribute name="src">
                                <xsl:call-template name="fo-external-image">
                                    <xsl:with-param name="filename" select="$JPL.logo.image"/>
                                </xsl:call-template>   
                            </xsl:attribute>
                        </fo:external-graphic>
                    </fo:block>
                    <fo:block text-align="left" color="gray" ><xsl:value-of select="$JPL.logo.text1"/></fo:block> 
                    <fo:block font-size="10pt"  text-align="left" font-style="italic" color="gray" space-after="0.25in"><xsl:value-of select="$JPL.logo.text2"/></fo:block> 
                    <fo:block text-align="center" font-size="9pt" font-style="italic"><xsl:value-of select="$jpl.footer"/></fo:block>
                </xsl:when>
                <xsl:otherwise>
                    <fo:block/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:template>
    <xsl:template name="foot.sep.rule">
        <xsl:param name="pageclass" select="''"/>
        <xsl:param name="sequence" select="''"/>
        <xsl:param name="gentext-key" select="''"/>
        <xsl:if test="$footer.rule !=0">
            <xsl:choose>
                <xsl:when test="$pageclass= 'jpl.title' and $sequence ='first'">
                    
                </xsl:when>
                <xsl:otherwise>
                    <xsl:attribute name="border-top-width">0.5pt</xsl:attribute>
                    <xsl:attribute name="border-top-style">solid</xsl:attribute>
                    <xsl:attribute name="border-top-color">black</xsl:attribute>
                </xsl:otherwise>
            </xsl:choose>
            
        </xsl:if>
    </xsl:template>
    
    <xsl:template name="head.sep.rule">
        <xsl:param name="pageclass" select="''"/>
        <xsl:param name="sequence" select="''"/>
        <xsl:param name="gentext-key" select="''"/>
        <xsl:if test="$header.rule !=0">
            <xsl:choose>
                <xsl:when test="$pageclass= 'jpl.title' and $sequence ='first'">
                    
                </xsl:when>
                <xsl:otherwise>
                    <xsl:attribute name="border-bottom-width">0.5pt</xsl:attribute>
                    <xsl:attribute name="border-bottom-style">solid</xsl:attribute>
                    <xsl:attribute name="border-bottom-color">black</xsl:attribute>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:if>
    </xsl:template>
    
    <xsl:template name="footer.table">
        <xsl:param name="pageclass" select="''"/>
        <xsl:param name="sequence" select="''"/>
        <xsl:param name="gentext-key" select="''"/>
        
        <!-- default is a single table style for all footers -->
        <!-- Customize it for different page classes or sequence location -->
        
        <xsl:choose>
            <xsl:when test="$pageclass = 'index'">
                <xsl:attribute name="margin-{$direction.align.start}">0pt</xsl:attribute>
            </xsl:when>
        </xsl:choose>
        
        <xsl:variable name="column1">
            <xsl:choose>
                <xsl:when test="$double.sided = 0">1</xsl:when>
                <xsl:when test="$sequence = 'first' or $sequence = 'odd'">1</xsl:when>
                <xsl:otherwise>3</xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        
        <xsl:variable name="column3">
            <xsl:choose>
                <xsl:when test="$double.sided = 0">3</xsl:when>
                <xsl:when test="$sequence = 'first' or $sequence = 'odd'">3</xsl:when>
                <xsl:otherwise>1</xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        
        <xsl:variable name="candidate">
            <xsl:choose>
                <xsl:when test="$pageclass='jpl.title'">
                    <fo:block>
                        <xsl:call-template name="footer.content">
                            <xsl:with-param name="pageclass" select="$pageclass"/>
                            <xsl:with-param name="sequence" select="$sequence"/>
                            <xsl:with-param name="gentext-key" select="$gentext-key"/>
                        </xsl:call-template>
                    </fo:block>
                </xsl:when>
                <xsl:otherwise>
                    <fo:table xsl:use-attribute-sets="footer.table.properties">
                        <xsl:call-template name="foot.sep.rule">
                            <xsl:with-param name="pageclass" select="$pageclass"/>
                            <xsl:with-param name="sequence" select="$sequence"/>
                            <xsl:with-param name="gentext-key" select="$gentext-key"/>
                        </xsl:call-template>
                        <fo:table-column column-number="1">
                            <xsl:attribute name="column-width">
                                <xsl:text>proportional-column-width(</xsl:text>
                                <xsl:call-template name="header.footer.width">
                                    <xsl:with-param name="location">footer</xsl:with-param>
                                    <xsl:with-param name="position" select="$column1"/>
                                    <xsl:with-param name="pageclass" select="$pageclass"/>
                                    <xsl:with-param name="sequence" select="$sequence"/>
                                    <xsl:with-param name="gentext-key" select="$gentext-key"/>
                                </xsl:call-template>
                                <xsl:text>)</xsl:text>
                            </xsl:attribute>
                        </fo:table-column>
                        <fo:table-column column-number="2">
                            <xsl:attribute name="column-width">
                                <xsl:text>proportional-column-width(</xsl:text>
                                <xsl:call-template name="header.footer.width">
                                    <xsl:with-param name="location">footer</xsl:with-param>
                                    <xsl:with-param name="position" select="2"/>
                                    <xsl:with-param name="pageclass" select="$pageclass"/>
                                    <xsl:with-param name="sequence" select="$sequence"/>
                                    <xsl:with-param name="gentext-key" select="$gentext-key"/>
                                </xsl:call-template>
                                <xsl:text>)</xsl:text>
                            </xsl:attribute>
                        </fo:table-column>
                        <fo:table-column column-number="3">
                            <xsl:attribute name="column-width">
                                <xsl:text>proportional-column-width(</xsl:text>
                                <xsl:call-template name="header.footer.width">
                                    <xsl:with-param name="location">footer</xsl:with-param>
                                    <xsl:with-param name="position" select="$column3"/>
                                    <xsl:with-param name="pageclass" select="$pageclass"/>
                                    <xsl:with-param name="sequence" select="$sequence"/>
                                    <xsl:with-param name="gentext-key" select="$gentext-key"/>
                                </xsl:call-template>
                                <xsl:text>)</xsl:text>
                            </xsl:attribute>
                        </fo:table-column>
                        
                        <fo:table-body>
                            <fo:table-row>
                                <xsl:attribute name="block-progression-dimension.minimum">
                                    <xsl:value-of select="$footer.table.height"/>
                                </xsl:attribute>
                                <fo:table-cell text-align="start"
                                    display-align="after">
                                    <xsl:if test="$fop.extensions = 0">
                                        <xsl:attribute name="relative-align">baseline</xsl:attribute>
                                    </xsl:if>
                                    <fo:block>
                                        <xsl:call-template name="footer.content">
                                            <xsl:with-param name="pageclass" select="$pageclass"/>
                                            <xsl:with-param name="sequence" select="$sequence"/>
                                            <xsl:with-param name="position" select="$direction.align.start"/>
                                            <xsl:with-param name="gentext-key" select="$gentext-key"/>
                                        </xsl:call-template>
                                    </fo:block>
                                </fo:table-cell>
                                <fo:table-cell text-align="center"
                                    display-align="after">
                                    <xsl:if test="$fop.extensions = 0">
                                        <xsl:attribute name="relative-align">baseline</xsl:attribute>
                                    </xsl:if>
                                    <fo:block>
                                        <xsl:call-template name="footer.content">
                                            <xsl:with-param name="pageclass" select="$pageclass"/>
                                            <xsl:with-param name="sequence" select="$sequence"/>
                                            <xsl:with-param name="position" select="'center'"/>
                                            <xsl:with-param name="gentext-key" select="$gentext-key"/>
                                        </xsl:call-template>
                                    </fo:block>
                                </fo:table-cell>
                                <fo:table-cell text-align="end"
                                    display-align="after">
                                    <xsl:if test="$fop.extensions = 0">
                                        <xsl:attribute name="relative-align">baseline</xsl:attribute>
                                    </xsl:if>
                                    <fo:block>
                                        <xsl:call-template name="footer.content">
                                            <xsl:with-param name="pageclass" select="$pageclass"/>
                                            <xsl:with-param name="sequence" select="$sequence"/>
                                            <xsl:with-param name="position" select="$direction.align.end"/>
                                            <xsl:with-param name="gentext-key" select="$gentext-key"/>
                                        </xsl:call-template>
                                    </fo:block>
                                </fo:table-cell>
                            </fo:table-row>
                        </fo:table-body>
                    </fo:table>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        
        <!-- Really output a footer? -->
        <xsl:choose>
            <xsl:when test="$sequence = 'blank' and $footers.on.blank.pages = 0">
                <!-- no output -->
            </xsl:when>
            <xsl:otherwise>
                <xsl:copy-of select="$candidate"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    <!-- make pdf links blue and underline -->
    <xsl:attribute-set name="xref.properties">
        <xsl:attribute name="color">blue</xsl:attribute>
        <xsl:attribute name="text-decoration">underline</xsl:attribute>
    </xsl:attribute-set>
    
    <!-- chapter and appendix name customization -->
    <xsl:param name="local.l10n.xml" select="document('')"/>
    <l:i18n xmlns:l="http://docbook.sourceforge.net/xmlns/l10n/1.0"> 
        <l:l10n language="en">
            <l:context name="title-numbered">
                <l:template name="chapter" text="Section %n. %t"/> 
                <l:template name="appendix" text="Appendix %n. %t"/> 
            </l:context>
        </l:l10n> 
    </l:i18n>
    
    <!-- chapter becomes section, appendix shows appendix work, they are bolded -->
    <xsl:template name="toc.line">
        <xsl:param name="toc-context" select="NOTANODE"/>
        <xsl:variable name="id">
            <xsl:call-template name="object.id"/>
        </xsl:variable>
    
        <xsl:variable name="label">
            <xsl:apply-templates select="." mode="label.markup"/>
        </xsl:variable>
    
        <fo:block xsl:use-attribute-sets="toc.line.properties">
            <fo:inline keep-with-next.within-line="always">
                <fo:basic-link internal-destination="{$id}">
                    <xsl:choose>
                        <xsl:when test="local-name(.) = 'chapter'">
                            <xsl:attribute name="font-weight">bold</xsl:attribute>
                            <xsl:call-template name="gentext"><xsl:with-param name="key" select="'section'"/></xsl:call-template>
                            <xsl:text> </xsl:text>
                        </xsl:when>
                        <xsl:when test="local-name(.) = 'appendix'">
                            <xsl:attribute name="font-weight">bold</xsl:attribute>
                            <xsl:call-template name="gentext"><xsl:with-param name="key" select="'appendix'"/></xsl:call-template>
                            <xsl:text> </xsl:text>
                        </xsl:when>
                    </xsl:choose>
                    <xsl:if test="$label != ''">
                        <xsl:copy-of select="$label"/>
                        <xsl:value-of select="$autotoc.label.separator"/>
                    </xsl:if>
                    <xsl:apply-templates select="." mode="titleabbrev.markup"/>
                </fo:basic-link>
            </fo:inline>
            <fo:inline keep-together.within-line="always">
                <xsl:text> </xsl:text>
                <fo:leader leader-pattern="dots" leader-pattern-width="3pt" leader-alignment="reference-area" keep-with-next.within-line="always"/>
                <xsl:text> </xsl:text> 
                <fo:basic-link internal-destination="{$id}">
                    <fo:page-number-citation ref-id="{$id}"/>
                </fo:basic-link>
            </fo:inline>
        </fo:block>
    </xsl:template>
    
    <!-- captions should be bold -->
    <xsl:template match="d:caption">
      <fo:block font-weight="bold">
        <xsl:apply-templates/>
      </fo:block>
    </xsl:template>
        
    <!-- added call to caption after the table -->
    <xsl:template name="calsTable">
        <xsl:variable name="keep.together">
            <xsl:call-template name="pi.dbfo_keep-together"/>
        </xsl:variable>
        <xsl:for-each select="d:tgroup">
            <fo:table xsl:use-attribute-sets="table.table.properties">
                <xsl:if test="$keep.together != ''">
                    <xsl:attribute name="keep-together.within-column">
                        <xsl:value-of select="$keep.together"/>
                    </xsl:attribute>
                </xsl:if>
                <xsl:call-template name="table.frame"/>
                <xsl:if test="following-sibling::d:tgroup">
                    <xsl:attribute name="border-bottom-width">0pt</xsl:attribute>
                    <xsl:attribute name="border-bottom-style">none</xsl:attribute>
                    <xsl:attribute name="padding-bottom">0pt</xsl:attribute>
                    <xsl:attribute name="margin-bottom">0pt</xsl:attribute>
                    <xsl:attribute name="space-after">0pt</xsl:attribute>
                    <xsl:attribute name="space-after.minimum">0pt</xsl:attribute>
                    <xsl:attribute name="space-after.optimum">0pt</xsl:attribute>
                    <xsl:attribute name="space-after.maximum">0pt</xsl:attribute>
                </xsl:if>
                <xsl:if test="preceding-sibling::d:tgroup">
                    <xsl:attribute name="border-top-width">0pt</xsl:attribute>
                    <xsl:attribute name="border-top-style">none</xsl:attribute>
                    <xsl:attribute name="padding-top">0pt</xsl:attribute>
                    <xsl:attribute name="margin-top">0pt</xsl:attribute>
                    <xsl:attribute name="space-before">0pt</xsl:attribute>
                    <xsl:attribute name="space-before.minimum">0pt</xsl:attribute>
                    <xsl:attribute name="space-before.optimum">0pt</xsl:attribute>
                    <xsl:attribute name="space-before.maximum">0pt</xsl:attribute>
                </xsl:if>
                <xsl:apply-templates select="."/>
            </fo:table>
            <xsl:for-each select="d:mediaobject|d:graphic">
                <xsl:apply-templates select="."/>
            </xsl:for-each>
        </xsl:for-each>
        <xsl:apply-templates select="d:caption"/>
    </xsl:template>
        
    <!-- pad 1 empty pages to make toc with page number v -->
    <xsl:template name="book.titlepage.separator">
        <xsl:if test="d:info/d:keywordset">
        <fo:block xmlns:fo="http://www.w3.org/1999/XSL/Format" break-after="page">
            <xsl:text> </xsl:text>
        </fo:block>
        <fo:block space-after="4in"/>
        <fo:block break-after="page" text-align="center" font-weight="bold">
            THIS PAGE INTENTIONALLY LEFT BLANK
        </fo:block>
        </xsl:if>
    </xsl:template>
    

    
    
    <!-- Updated SBUC Front matter -->
    <xsl:template name="book.titlepage.before.recto">
        <xsl:if test="d:info/d:cover/d:mediaobject">
            <xsl:apply-templates select="d:info/d:cover/d:mediaobject"/>
            <fo:block break-after="page"/>
        </xsl:if>
    </xsl:template>
    
    <!-- Title Page -->
    
        
    <xsl:template name="book.titlepage.recto">
        <xsl:if test="d:info/d:mediaobject">
            <xsl:apply-templates select="d:info/d:mediaobject"/>
        </xsl:if>
        <fo:block text-align="center" font-size="24pt" space-after="0.5in"> 
            <xsl:value-of select="d:info/d:title"/>
        </fo:block>
        <fo:block text-align="center" font-size="18pt"> 
            <xsl:value-of select="d:info/d:subtitle"/>
        </fo:block>
        <fo:block text-align="center" font-size="12pt"> 
            <xsl:value-of select="d:info/d:releaseinfo"/>
        </fo:block>
        <fo:block-container absolute-position="absolute" top="6.4in" left="0in">
        <fo:block text-align="left" font-size="9pt">
            Paper copies of this document may not be current and should not be relied on for official purposes. The current version is availible from <xsl:value-of select="d:info/d:publisher/d:publishername"/> online at:</fo:block>
        <fo:block text-align="left" font-size="9pt" space-after="0.25in">
            <xsl:value-of select="d:info/d:publisher/d:address"/>
        </fo:block>
        <fo:block text-align="left" font-size="12pt">
            <xsl:value-of select="d:info/d:legalnotice/d:title"/>
        </fo:block>
        <fo:block text-align="left" font-size="10pt">
            <xsl:value-of select="d:info/d:pubdate"/>
        </fo:block>
        <fo:block text-align="left" font-size="10pt">
            <xsl:value-of select="d:info/d:productnumber"/>
        </fo:block>
        </fo:block-container>
        <fo:block break-after="page"/>
    </xsl:template> 
    
    <!--Signature Page-->
    <xsl:template name="book.titlepage.before.verso">
        <xsl:if test="d:info/d:author or d:info/d:editor or d:info/d:othercredit">
            <fo:block text-align="left" font-size="18pt" space-before="0.25in"> 
                <xsl:value-of select="d:info/d:title"/>
            </fo:block>
            <fo:block text-align="left" font-size="18pt"> 
                <xsl:value-of select="d:info/d:subtitle"/>
            </fo:block>
            <fo:block text-align="left" font-size="12pt" space-after="0.5in"> 
                <xsl:value-of select="d:info/d:releaseinfo"/>
            </fo:block>
            <xsl:if test="d:info/d:author">
                <fo:block font-size="12pt" space-after="0.25in ">
                    PREPARED BY:
                </fo:block>
                <fo:block space-after="0.5in">
                    <xsl:apply-templates select="d:info/d:author"/>
                </fo:block>
                </xsl:if>
            <xsl:if test="d:info/d:editor">
                <fo:block font-size="12pt" space-after="0.25in ">
                    APPROVED BY:
                </fo:block>
                <fo:block space-after="0.5in">
                    <xsl:apply-templates select="d:info/d:editor"/>
                </fo:block>
            </xsl:if>
            <xsl:if test="d:info/d:othercredit">
                <fo:block font-size="12pt" space-after="0.25in ">
                    CONFIRMED BY:
                </fo:block>
                    <fo:block space-after="0.5in">
                    <xsl:apply-templates select="d:info/d:othercredit"/>
                </fo:block>
            </xsl:if>
            <fo:block break-after="page"/>
        </xsl:if>
    </xsl:template>
    
    <!-- Revision History Page-->
    <xsl:template name="book.titlepage.verso">
       <xsl:if test="d:info/d:revhistory">
            <fo:block text-align="center" font-weight="bold" space-before="0.25in" space-after="0.25in">Change Log</fo:block>
            <fo:table table-layout="fixed" border-width="0.5mm" border-style="solid">
                <fo:table-column column-number="1" column-width="15%"/>
                <fo:table-column column-number="2" column-width="10%"/>
                <fo:table-column column-number="3" column-width="50%"/>
                <fo:table-column column-number="4" column-width="25%"/>
                <fo:table-body>
                    <fo:table-row  background-color="grey">
                        <fo:table-cell>
                            <fo:block font-weight="bold" text-align="center">
                                Version
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                            <fo:block font-weight="bold" text-align="center">
                                Date
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                            <fo:block font-weight="bold" text-align="center">
                                Sections Changed
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                            <fo:block font-weight="bold" text-align="center">
                                Author
                            </fo:block>
                        </fo:table-cell>
                    </fo:table-row>
                    <xsl:apply-templates select="d:info/d:revhistory/d:revision"/>
                </fo:table-body>
            </fo:table>
       </xsl:if>
       <xsl:if test="d:info/d:address">
            <fo:block text-align="center" font-weight="bold" space-before="0.25in" space-after="0.25in">Distribution List</fo:block> 
            <xsl:apply-templates select="d:info/d:address"/>
       </xsl:if>
   </xsl:template>
    
    <!-- template for collaborator emails -->
    <xsl:template match="d:info/d:address">
        <fo:block>
            <xsl:value-of select="d:email"/>
        </fo:block>        
    </xsl:template>
    
    <!-- template to support revision history table -->
    <xsl:template match="d:info/d:revhistory/d:revision">
        <xsl:variable name="revnumber" select="d:revnumber"/>
        <xsl:variable name="revdate"   select="d:date"/>
        <xsl:variable name="revauthor"   select="d:author/d:personname"/>
        <xsl:variable name="revremark" select="d:revremark"/>
        <fo:table-row>
            
            <fo:table-cell padding="1mm" border-width="0.5mm" border-style="solid" >
                <fo:block text-align="center">
                    <xsl:call-template name="anchor"/>
                    <xsl:if test="$revnumber">
                        <xsl:call-template name="gentext.space"/>
                        <xsl:apply-templates select="$revnumber[1]"/>
                    </xsl:if>
                </fo:block>
            </fo:table-cell>
            
            <fo:table-cell padding="1mm" border-width="0.5mm" border-style="solid">
                <fo:block text-align="center">
                    <xsl:apply-templates select="$revdate[1]"/>
                </fo:block>
            </fo:table-cell>
            
            <fo:table-cell padding="1mm" border-width="0.5mm" border-style="solid">
                <fo:block>
                    <xsl:if test="$revremark">
                        <fo:block>
                            <xsl:apply-templates select="$revremark[1]" />
                        </fo:block>
                    </xsl:if>
                </fo:block>
            </fo:table-cell>
            <fo:table-cell padding="1mm" border-width="0.5mm" border-style="solid">
                <fo:block text-align="center">
                    <xsl:if test="$revauthor">
                        <xsl:value-of select="d:author/d:personname/d:firstname"/>
                        <xsl:call-template name="gentext.space"/>
                        <xsl:value-of select="d:author/d:personname/d:surname"/>
                    </xsl:if>
                </fo:block>
            </fo:table-cell>
        </fo:table-row>
    </xsl:template>
     
    <!-- Templates to support dynamic calling of signatures --> 
    <xsl:template match="d:info/d:author"> 
            <xsl:call-template name="signature.author"/>
    </xsl:template>
    
    <xsl:template match="d:info/d:editor">
        <xsl:call-template name="signature.editor"/>
    </xsl:template>
    
    <xsl:template match="d:info/d:othercredit">
        <xsl:call-template name="signature.othercredit"/>
    </xsl:template>
    
    <!--Locates Authors "Preparing Engineer(s)" -->
    <xsl:template name="signature.author">            
            <fo:block space-before="0.25in">
                <fo:table>
                    <fo:table-column column-number="1" column-width="75%"/>
                    <fo:table-column column-number="2" column-width="25%"/>
                    <fo:table-body>
                        <fo:table-row>
                            <fo:table-cell>
                                <fo:block>
                                    ________________________________________________
                                </fo:block>
                            </fo:table-cell>
                            <fo:table-cell>
                                <fo:block>
                                    _________________________
                                </fo:block>
                            </fo:table-cell>
                        </fo:table-row>
                        <fo:table-row>
                            <fo:table-cell font-size="12pt" text-align="left">
                                <fo:block>
                                    <xsl:value-of select="d:personname/d:firstname"/>
                                    
                                    <xsl:value-of select="d:personname/d:surname"/>
                                </fo:block>
                                <fo:block>
                                    <xsl:value-of select="d:affiliation/d:jobtitle"/>,
                                    <xsl:value-of select="d:affiliation/d:org/d:orgname"/>
                                    (<xsl:value-of select="d:affiliation/d:org/d:orgdiv"/>)
                                </fo:block>
                            </fo:table-cell>
                            <fo:table-cell>
                                <fo:block>Date</fo:block>
                            </fo:table-cell>
                        </fo:table-row>
                    </fo:table-body>
                </fo:table>      
            </fo:block>
    </xsl:template>
    
    <!--Locates Editors "Approving Engineer(s)" -->  
    <xsl:template name="signature.editor">
        <fo:block space-before="0.25in">
            <fo:table>
                <fo:table-column column-number="1" column-width="75%"/>
                <fo:table-column column-number="2" column-width="25%"/>
                <fo:table-body>
                    <fo:table-row>
                        <fo:table-cell>
                            <fo:block>
                                ________________________________________________
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                            <fo:block>
                                _________________________
                            </fo:block>
                        </fo:table-cell>
                    </fo:table-row>
                    <fo:table-row>
                        <fo:table-cell font-size="12pt" text-align="left">
                            <fo:block>
                                <xsl:value-of select="d:personname/d:firstname"/>
                                <xsl:call-template name="gentext.space"/>
                                <xsl:value-of select="d:personname/d:surname"/>
                            </fo:block>
                            <fo:block>
                                <xsl:value-of select="d:affiliation/d:jobtitle"/>,
                                <xsl:value-of select="d:affiliation/d:org/d:orgname"/>
                                (<xsl:value-of select="d:affiliation/d:org/d:orgdiv"/>)
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                            <fo:block>Date</fo:block>
                        </fo:table-cell>
                    </fo:table-row>
                </fo:table-body>
            </fo:table>      
        </fo:block>    
    </xsl:template>
    
    <!--Locates OtherCredit blocks "Concuring Engineer(s)" -->
    <xsl:template name="signature.othercredit">
        <fo:block space-before="0.25in">
            <fo:table>
                <fo:table-column column-number="1" column-width="75%"/>
                <fo:table-column column-number="2" column-width="25%"/>
                <fo:table-body>
                    <fo:table-row>
                        <fo:table-cell>
                            <fo:block>
                                ________________________________________________
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                            <fo:block>
                                _________________________
                            </fo:block>
                        </fo:table-cell>
                    </fo:table-row>
                    <fo:table-row>
                        <fo:table-cell font-size="12pt" text-align="left">
                            <fo:block>
                                <xsl:value-of select="d:personname/d:firstname"/>
                                <xsl:call-template name="gentext.space"/>
                                <xsl:value-of select="d:personname/d:surname"/>
                            </fo:block>
                            <fo:block>
                                <xsl:value-of select="d:affiliation/d:jobtitle"/>,
                                <xsl:value-of select="d:affiliation/d:org/d:orgname"/>
                                (<xsl:value-of select="d:affiliation/d:org/d:orgdiv"/>)
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                            <fo:block>Date</fo:block>
                        </fo:table-cell>
                    </fo:table-row>
                </fo:table-body>
            </fo:table>      
        </fo:block>
   </xsl:template>
   
   <!-- decrease literallayout and monospace font in pdfs so they don't run off the page -->
   <xsl:attribute-set name="monospace.verbatim.properties" use-attribute-sets="verbatim.properties monospace.properties">
     	<xsl:attribute name="text-align">start</xsl:attribute>
     	<xsl:attribute name="wrap-option">no-wrap</xsl:attribute>
     	<xsl:attribute name="font-size">7pt</xsl:attribute>
   	</xsl:attribute-set>
</xsl:stylesheet>