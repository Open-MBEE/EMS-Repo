<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
                xmlns:d="http://docbook.org/ns/docbook"
                version="1.0">
  <xsl:import href="profile-docbook.xsl"/>
  <!-- Apply XHLTHL extension. -->
  <xsl:import href="highlight.xsl"/>
  <xsl:import href="../oxygen_custom_html.xsl"/>
  <xsl:import href="chunk-common.xsl"/>
  <xsl:include href="manifest.xsl"/>
  <xsl:include href="profile-chunk-code.xsl"/>
  
  
  <xsl:param name="generate.toc">
    appendix  toc,title
    article/appendix  nop
    article   toc,title
    book      toc,title,figure,table,example,equation
    chapter   nop
    part      toc,title
    preface   toc,title
    qandadiv  toc
    qandaset  toc
    reference toc,title
    sect1     toc
    sect2     toc
    sect3     toc
    sect4     toc
    sect5     toc
    section   nop
    set       toc,title
    </xsl:param>

   <!-- doris patch to add header and footer -->
    <xsl:param name="jpl.header" select="''"/>
    <xsl:param name="jpl.footer" select="''"/>
    
    <xsl:param name="jpl.subheader" select="''"/>
    <xsl:param name="jpl.subfooter" select="''"/>
    
    <xsl:param name="toc.section.depth" select="8"/>
    <xsl:param name="section.label.includes.component.label" select="1"/>
    <xsl:param name="section.autolabel" select="1"/>
    
    <xsl:param name="para.propagates.style" select="1"/>
    <xsl:param name="phrase.propagates.style" select="1"/>
    <xsl:param name="entry.propagates.style" select="1"/>
    <xsl:param name="emphasis.propagates.style" select="1"/>
    
    <xsl:template name="user.header.navigation">
        <xsl:if test="$jpl.header != ''">
            <div align="center"><span><xsl:value-of select="$jpl.header"/></span></div>
        </xsl:if>
        <xsl:if test="$jpl.subheader != ''">
            <div align="center"><span><xsl:value-of select="$jpl.subheader"/></span></div>
        </xsl:if>
    </xsl:template>
    <xsl:template name="user.footer.navigation">
        <xsl:if test="$jpl.subfooter != ''">
            <div align="center"><span><xsl:value-of select="$jpl.subfooter"/></span></div>
        </xsl:if>
        <xsl:if test="$jpl.footer != ''">
            <div align="center"><span><xsl:value-of select="$jpl.footer"/></span></div>
        </xsl:if>
    </xsl:template>
  
    <xsl:template name="breadcrumbs">
        <xsl:param name="this.node" select="."/>
        <div class="breadcrumbs">
            <xsl:for-each select="$this.node/ancestor::*">
                <span class="breadcrumb-link">
                    <a>
                        <xsl:attribute name="href">
                            <xsl:call-template name="href.target">
                                <xsl:with-param name="object" select="."/>
                                <xsl:with-param name="context" select="$this.node"/>
                            </xsl:call-template>
                        </xsl:attribute>
                        <xsl:apply-templates select="." mode="title.markup"/>
                    </a>
                </span>
                <xsl:text> &gt; </xsl:text>
            </xsl:for-each>
      <!-- And display the current node, but not as a link -->
            <span class="breadcrumb-node">
                <xsl:apply-templates select="$this.node" mode="title.markup"/>
            </span>
        </div>
    </xsl:template>
 
    <xsl:param name="target.window" select="'body'"/>
    
    <xsl:template name="user.head.content">
        <base target="{$target.window}"/>
    </xsl:template>

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
      <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:bookinfo/d:othercredit"/>
      <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:info/d:othercredit"/>
      <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:bookinfo/d:releaseinfo"/>
      <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:info/d:releaseinfo"/>
      <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:bookinfo/d:copyright"/>
      <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:info/d:copyright"/>
      <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:bookinfo/d:cover"/>
      <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:info/d:cover"/>
      <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:bookinfo/d:legalnotice"/>
      <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:info/d:legalnotice"/>
      <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:bookinfo/d:pubdate"/>
      <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:info/d:pubdate"/>
      <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:bookinfo/d:revision"/>
      <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:info/d:revision"/>
      <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:bookinfo/d:revhistory"/>
      <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:info/d:revhistory"/>
      <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:bookinfo/d:abstract"/>
      <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:info/d:abstract"/>
    </xsl:template>
    
    <xsl:template match="d:cover" mode="book.titlepage.recto.auto.mode">
        <div xsl:use-attribute-sets="book.titlepage.recto.style">
        <xsl:apply-templates select="." mode="book.titlepage.recto.mode"/>
        </div>
    </xsl:template>
    
    <xsl:template match="d:cover" mode="titlepage.mode">
        <xsl:apply-templates mode="titlepage.mode"/>
    </xsl:template>
    
    <xsl:param name="local.l10n.xml" select="document('')"/>
    <l:i18n xmlns:l="http://docbook.sourceforge.net/xmlns/l10n/1.0"> 
        <l:l10n language="en">
            <l:context name="title-numbered">
                <l:template name="chapter" text="%n.&#160;%t"/> 
            </l:context>
        </l:l10n> 
    </l:i18n>
     
    <!--   
    <xsl:template match="d:caption">
        <div>
        <xsl:apply-templates select="." mode="common.html.attributes"/>
        <xsl:if test="@align = 'right' or @align = 'left' or @align='center'">
            <xsl:attribute name="align"><xsl:value-of select="@align"/></xsl:attribute>
        </xsl:if>
        <xsl:apply-templates/>
        </div>
    </xsl:template> -->
    
</xsl:stylesheet>
