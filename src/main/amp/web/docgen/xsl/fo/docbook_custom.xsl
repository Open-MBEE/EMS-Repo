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

    <!-- suppress showing url after links -->
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
            
    <xsl:template name="header.content">
        <xsl:param name="pageclass" select="''"/>
        <xsl:param name="sequence" select="''"/>
        <xsl:param name="position" select="''"/>
        <xsl:param name="gentext-key" select="''"/>

        <!-- sequence can be odd, even, first, blank -->
        <!-- position can be left, center, right -->
        <xsl:choose>
            <xsl:when test="$sequence = 'blank'">
                <fo:block></fo:block>
            </xsl:when>

            <xsl:when test="$position='left'">
                    <!-- Same for odd, even, empty, and blank sequences -->
                <xsl:call-template name="draft.text"/>
            </xsl:when>

            <xsl:when test="$position='center'">
                <xsl:if test="$jpl.header!=''">
                    <fo:block font-size="9pt"><xsl:value-of select="$jpl.header"/></fo:block>
                </xsl:if>
                <xsl:if test="$jpl.subheader!=''">
                    <fo:block font-size="7pt"><xsl:value-of select="$jpl.subheader"/></fo:block>
                </xsl:if>
            </xsl:when>

            <xsl:when test="$position='right'">
                    <!-- Same for odd, even, empty, and blank sequences -->
                <fo:block><xsl:call-template name="draft.text"/></fo:block>
            </xsl:when>
            <xsl:otherwise>
                <fo:block></fo:block>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    <xsl:template name="footer.content">
        <xsl:param name="pageclass" select="''"/>
        <xsl:param name="sequence" select="''"/>
        <xsl:param name="position" select="''"/>
        <xsl:param name="gentext-key" select="''"/>

        <!-- pageclass can be front, body, back -->
        <!-- sequence can be odd, even, first, blank -->
        <!-- position can be left, center, right -->
        <xsl:choose>
            <xsl:when test="$pageclass = 'titlepage' or $pageclass = 'lot'">
                <fo:block></fo:block>
            </xsl:when>
                  
            <xsl:when test="$double.sided != 0 and $sequence = 'even' and $position='left'">
                <fo:block><fo:page-number/></fo:block>
            </xsl:when>

            <xsl:when test="$double.sided != 0 and ($sequence = 'odd' or $sequence = 'first') and $position='right'">
                <fo:block><fo:page-number/></fo:block>
            </xsl:when>

            <xsl:when test="$double.sided = 0 and $position='center'">
                <fo:block><fo:page-number/></fo:block>
                <xsl:if test="$jpl.subfooter!=''">
                    <fo:block font-size="7pt" font-style="italic"><xsl:value-of select="$jpl.subfooter"/></fo:block>
                </xsl:if>
                <xsl:if test="$jpl.footer!=''">
                    <fo:block font-size="9pt" font-style="italic"><xsl:value-of select="$jpl.footer"/></fo:block>
                </xsl:if>
            </xsl:when>

            <xsl:when test="$sequence='blank'">
                <xsl:choose>
                    <xsl:when test="$double.sided != 0 and $position = 'left'">
                        <fo:block><fo:page-number/></fo:block>
                    </xsl:when>
                    <xsl:when test="$double.sided = 0 and $position = 'center'">
                        <fo:block><fo:page-number/></fo:block>
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
    
    <!-- make Chapters Section and show Appendix words -->
    <xsl:param name="local.l10n.xml" select="document('')"/>
    <l:i18n xmlns:l="http://docbook.sourceforge.net/xmlns/l10n/1.0"> 
        <l:l10n language="en">
            <l:context name="title-numbered">
                <l:template name="chapter" text="%n.&#160;%t"/> 
            </l:context>
        </l:l10n> 
    </l:i18n>
              
   
    
    <!-- from unknown, make captions bold -->
    <xsl:template match="d:caption">
        <fo:block font-weight="bold">
            <xsl:apply-templates/>
        </fo:block>
    </xsl:template>
    
    <!-- from table.xsl, shows table caption -->
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
        <xsl:apply-templates select="d:caption"/> <!-- added -->
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

    <xsl:template match="d:legalnotice" mode="book.titlepage.recto.auto.mode">
        <fo:block xmlns:fo="http://www.w3.org/1999/XSL/Format" xsl:use-attribute-sets="book.titlepage.recto.style">
            <xsl:apply-templates/>
        </fo:block>
    </xsl:template>
</xsl:stylesheet>