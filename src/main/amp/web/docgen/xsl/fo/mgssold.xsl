<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
    xmlns:d="http://docbook.org/ns/docbook"
    xmlns:fo="http://www.w3.org/1999/XSL/Format"
    version="1.0">
    <xsl:import href="profile-docbook.xsl"/>
    <!-- Apply XHLTHL extension. -->
    <xsl:import href="highlight.xsl"/>
    <xsl:import href="../oxygen_custom.xsl"/>
    
    
    <xsl:param name="hard.pagebreak" select="false"/>
    <xsl:template match="processing-instruction('hard-pagebreak')">
        <xsl:if test="$hard.pagebreak='true'">
            <fo:block break-after='page'/>
        </xsl:if>
    </xsl:template>
    
    <!-- param customizations, ulink.show suppresses showing url after links -->
    <xsl:param name="ulink.show" select="0"/>
    <xsl:param name="jpl.header" select="''"/>
    <xsl:param name="jpl.footer" select="''"/>
    <xsl:param name="jpl.subheader" select="''"/>
    <xsl:param name="jpl.subfooter" select="''"/>

    <xsl:param name="toc.section.depth" select="8"/>
    <xsl:param name="section.label.includes.component.label" select="1"/>
    <xsl:param name="section.autolabel" select="1"/>
    <xsl:param name="body.start.indent" select="1"/>
    
    <xsl:param name="header.column.widths">1 20 1</xsl:param>
    <xsl:param name="footer.column.widths">1 20 1</xsl:param>
    
    <!-- header is smaller -->
        <xsl:template name="header.content">
            <xsl:param name="pageclass" select="''"/>
            <xsl:param name="sequence" select="''"/>
            <xsl:param name="position" select="''"/>
            <xsl:param name="gentext-key" select="''"/>

            <!-- sequence can be odd, even, first, blank -->
            <!-- position can be left, center, right -->
            <xsl:choose>

            	<xsl:when test="$position='center'">
                    <xsl:if test="$jpl.header!=''">
                        <fo:block font-size="9pt"><xsl:value-of select="$jpl.header"/></fo:block>
                    </xsl:if>
                    <xsl:if test="$jpl.subheader!=''">
                        <fo:block font-size="7pt"><xsl:value-of select="$jpl.subheader"/></fo:block>
                    </xsl:if>
                </xsl:when>
                <xsl:otherwise>
                    <fo:block></fo:block>
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
            	<xsl:when test="$position='center' and $pageclass != 'titlepage'">
                    <fo:block><fo:page-number/></fo:block>
                    <xsl:if test="$jpl.subfooter!=''">
                        <fo:block font-size="7pt" font-style="italic"><xsl:value-of select="$jpl.subfooter"/></fo:block>
                    </xsl:if>
                    <xsl:if test="$jpl.footer!=''">
                        <fo:block font-size="9pt" font-style="italic"><xsl:value-of select="$jpl.footer"/></fo:block>
                    </xsl:if>
                </xsl:when>

                <xsl:when test="$pageclass = 'titlepage' and $position='center'">
                    <xsl:if test="$jpl.subfooter!=''">
                        <fo:block font-size="7pt" font-style="italic"><xsl:value-of select="$jpl.subfooter"/></fo:block>
                    </xsl:if>
                    <xsl:if test="$jpl.footer!=''">
                        <fo:block font-size="9pt" font-style="italic"><xsl:value-of select="$jpl.footer"/></fo:block>
                    </xsl:if>
                </xsl:when>

                <xsl:otherwise>
                    <fo:block></fo:block>
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
                <l:template name="chapter" text="Section %n.&#160;%t"/> 
                <l:template name="appendix" text="Appendix %n.&#160;%t"/> 
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
                <fo:leader leader-pattern="dots"
                    leader-pattern-width="3pt"
                    leader-alignment="reference-area"
                    keep-with-next.within-line="always"/>
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
        
    <!-- pad 2 empty pages to make toc with page number v -->
    <xsl:template name="book.titlepage.separator">
        <fo:block xmlns:fo="http://www.w3.org/1999/XSL/Format" break-after="page">
            <xsl:text>&#xA0;</xsl:text>
        </fo:block>
        <fo:block xmlns:fo="http://www.w3.org/1999/XSL/Format" break-after="page">
            <xsl:text>&#xA0;</xsl:text>
        </fo:block>
        <fo:block xmlns:fo="http://www.w3.org/1999/XSL/Format" break-after="page">
            <xsl:text>&#xA0;</xsl:text>
        </fo:block>
    </xsl:template>
    
    <!-- title page have cover and/or legalnotice -->
    <xsl:template name="book.titlepage.recto">
        <xsl:choose>
            <xsl:when test="d:bookinfo/d:title">
                <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:bookinfo/d:title"/>
            </xsl:when>
            <xsl:when test="d:info/d:title">
                <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:info/d:title"/>
            </xsl:when>
            <xsl:when test="d:title">
                <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:title"/>
            </xsl:when>
        </xsl:choose>

        <xsl:choose>
            <xsl:when test="d:bookinfo/d:subtitle">
                <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:bookinfo/d:subtitle"/>
            </xsl:when>
            <xsl:when test="d:info/d:subtitle">
                <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:info/d:subtitle"/>
            </xsl:when>
            <xsl:when test="d:subtitle">
                <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:subtitle"/>
            </xsl:when>
        </xsl:choose>

        <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:bookinfo/d:corpauthor"/>
        <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:info/d:corpauthor"/>
        <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:bookinfo/d:authorgroup"/>
        <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:info/d:authorgroup"/>
        <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:bookinfo/d:author"/>
        <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:info/d:author"/>
        <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:info/d:cover"/>
        <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:info/d:legalnotice"/>
    </xsl:template>
    
    <xsl:template match="d:cover" mode="book.titlepage.recto.auto.mode">
        <fo:block xmlns:fo="http://www.w3.org/1999/XSL/Format" xsl:use-attribute-sets="book.titlepage.recto.style">
            <xsl:apply-templates/>
        </fo:block>
    </xsl:template>

    <!-- decrease literallayout and monospace font in pdfs so they don't run off the page -->
    <xsl:attribute-set name="monospace.verbatim.properties" use-attribute-sets="verbatim.properties monospace.properties">
  		<xsl:attribute name="text-align">start</xsl:attribute>
  		<xsl:attribute name="wrap-option">no-wrap</xsl:attribute>
  		<xsl:attribute name="font-size">6pt</xsl:attribute>
	</xsl:attribute-set>
</xsl:stylesheet>