-----------------------
WS-I Testing Tools V1.1
-----------------------

This package contains a draft of the WS-I testing tools implementation for 
the Basic Profile V1.0, Basic Profile V1.1 and Simple Soap Binding Profile V1.0. 

Copyright (c) 2002-2004 by The Web Services-Interoperability Organization (WS-I) and 
Certain of its Members. All Rights Reserved.

--------
Contents
--------

  1. Notice
  2. WS-I License Information
  3. How to Provide Feedback
  4. Known problems
  5. Tool Directory Structure

---------
1. Notice
---------

The material contained herein is not a license, either expressly or impliedly, to any 
intellectual property owned or controlled by any of the authors or developers of this 
material or WS-I. The material contained herein is provided on an "AS IS" basis and to 
the maximum extent permitted by applicable law, this material is provided AS IS AND WITH 
ALL FAULTS, and the authors and developers of this material and WS-I hereby disclaim all 
other warranties and conditions, either express, implied or statutory, including, but not 
limited to, any (if any) implied warranties, duties or conditions of  merchantability, 
of fitness for a particular purpose, of accuracy or completeness of responses, of results, 
of workmanlike effort, of lack of viruses, and of lack of negligence. ALSO, THERE IS NO 
WARRANTY OR CONDITION OF TITLE, QUIET ENJOYMENT, QUIET POSSESSION, CORRESPONDENCE TO 
DESCRIPTION OR NON-INFRINGEMENT WITH REGARD TO THIS MATERIAL.

IN NO EVENT WILL ANY AUTHOR OR DEVELOPER OF THIS MATERIAL OR WS-I BE LIABLE TO ANY OTHER 
PARTY FOR THE COST OF PROCURING SUBSTITUTE GOODS OR SERVICES, LOST PROFITS, LOSS OF USE, 
LOSS OF DATA, OR ANY INCIDENTAL, CONSEQUENTIAL, DIRECT, INDIRECT, OR SPECIAL DAMAGES 
WHETHER UNDER CONTRACT, TORT, WARRANTY, OR OTHERWISE, ARISING IN ANY WAY OUT OF THIS OR 
ANY OTHER AGREEMENT RELATING TO THIS MATERIAL, WHETHER OR NOT SUCH PARTY HAD ADVANCE 
NOTICE OF THE POSSIBILITY OF SUCH DAMAGES.


---------------------------
2. WS-I License Information
---------------------------

Use of this WS-I Material is governed by the WS-I Test License at 
http://ws-i.org/docs/license/test_license.htm. By downloading 
these files, you agree to the terms of this license.


--------------------------
3. How To Provide Feedback
--------------------------

The Web Services-Interoperability Organization (WS-I) would like to receive input, 
suggestions and other feedback ("Feedback") on this work from a wide variety of 
industry participants to improve its quality over time. 

By sending email, or otherwise communicating with WS-I, you (on behalf of yourself if 
you are an individual, and your company if you are providing Feedback on behalf of the 
company) will be deemed to have granted to WS-I, the members of WS-I, and other parties 
that have access to your Feedback, a non-exclusive, non-transferable, worldwide, perpetual, 
irrevocable, royalty-free license to use, disclose, copy, license, modify, sublicense or 
otherwise distribute and exploit in any manner whatsoever the Feedback you provide regarding 
the work. You acknowledge that you have no expectation of confidentiality with respect to 
any Feedback you provide. You represent and warrant that you have rights to provide this 
Feedback, and if you are providing Feedback on behalf of a company, you represent and warrant 
that you have the rights to provide Feedback on behalf of your company. You also acknowledge 
that WS-I is not required to review, discuss, use, consider or in any way incorporate your 
Feedback into future versions of its work. If WS-I does incorporate some or all of your 
Feedback in a future version of the work, it may, but is not obligated to include your name 
(or, if you are identified as acting on behalf of your company, the name of your company) on 
a list of contributors to the work. If the foregoing is not acceptable to you and any company 
on whose behalf you are acting, please do not provide any Feedback.

Please refer to release notes included with each tool package, for the
details on what information to include when reporting a problem.

Feedback on this document should be directed to wsi-test-comments@ws-i.org.


---------------------------
4. Known problems
---------------------------

Please refer to the release notes associated with each tool package, for the details
of known problems with the tools.

A more up-to-date and detailed "tools errata" is also published and maintained on the 
WS-I site, independently from this tool package. This errata contains issues that will
be considered for the next release.

NOTE: due to namespaces, the Report.xsl is not backward compatible.

NOTE: There is no guarantee that the C# and Java tools will always produce the same conformance 
report from the same input material. In spite of extensive tool testing and alignment,
some discrepancies remain that are due to differences in the utilities
that are natively provided by each development platform.
The cases where discrepancies are expected are documented in the release notes, 
and detailed in the errata.  


---------------------------
5. Tool Directory Structure
---------------------------

\wsi-test-tools\README.txt - This file
               \License.htm - WS-I license 

\wsi-test-tools\common\docs - User's Guide, etc.
                      \profiles - BasicProfileTestAssertions.xml, BasicProfile_1.1_TAD.xml, SSBP10_BP11_TAD.xml.
                      \schemas - all XML schema documents 
                      \xsl - all XSL files

\wsi-test-tools\cs\releaseNotes.txt - C# release notes
                  \bin - executables, etc.
                  \samples - C# specific samples (log.xml, report.xml)

\wsi-test-tools\java\releaseNotes.txt - Java release notes
                    \bin - batch files and shell scripts
                    \lib - wsi-test-tools.jar, prereq Jar files
                    \licenses - licenses for prereq Jar files (Apache, etc.)
                    \samples - Java specific samples (log.xml, report.xml)
