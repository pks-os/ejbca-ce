<%@ page language="Java" import="javax.naming.*,javax.rmi.*,java.util.*,java.security.cert.*,se.anatom.ejbca.ca.sign.*"%>

<HTML>
<HEAD>
<TITLE>EJBCA Demo Certificate Enroll</TITLE>
<link rel="stylesheet" href="indexmall.css" type="text/css">
</HEAD>
<BODY bgcolor="#ffffff" link="black" vlink="black" alink="black">
<center>
  <FONT face=arial size="3"><strong><span class="E">E</span><span class="J">J</span><span class="B">B</span><span class="C">C</span><span class="A">A&nbsp;</span></strong></FONT> 
  <span class="titel">Demo Certificate Enrollment </span> 
</center>

<HR>
<div align="center">Welcome to certificate enrollment. <BR>
  If you haven't done so already, you should<br>
  first fetch the CA certificate(s). </div>
<P align="center">Fetch CA certificates: 
  <%
try  {
    InitialContext ctx = new InitialContext();
    ISignSessionHome home = home = (ISignSessionHome) PortableRemoteObject.narrow(
            ctx.lookup("RSASignSession"), ISignSessionHome.class );
    ISignSession ss = home.create();
    Certificate[] chain = ss.getCertificateChain();
    if (chain.length == 0) {
        out.println("No CA certificates exist");
    } else {
        out.println("<li><a href=\"/webdist/certdist?cmd=cacert&level=0\">Root CA</a></li>");
        if (chain.length > 1) {
            for (int i=chain.length-2;i>=0;i--) {
                out.println("<li><a href=\"/webdist/certdist?cmd=cacert&level="+i+"\">CA</a></li>");
            }
        }
    }
} catch(Exception ex) {
    ex.printStackTrace();
}                                             
%>
<hr align="center">
<div align="center">
  <script language="javaScript">
//*** This function, triggered by clicking the fake Submit button, checks
//*** for, and rejects, blank values in the name and email fields.

function validateForm() {
  var okSoFar=true //-- Changes to false when bad field found.
  //-- Check the common name field, reject if blank.
  if (document.demoreq.cn.value=="") {
    okSoFar=false
    alert("Please fill in the Common Name field!")
    document.demoreq.cn.focus()
  }
  document.demoreq.user.value=document.demoreq.dn.value+document.demoreq.cn.value

  if (document.demoreq.email.value!="") {
     //-- Reject email address if it doesn't contain an @ character.
      var foundAt = document.demoreq.email.value.indexOf("@",0)
      if (foundAt < 1 && okSoFar) {
        okSoFar = false
        alert ("EMail address should contain an @ character!")
        document.demoreq.email.focus()
      }
  document.demoreq.user.value=document.demoreq.user.value+",EmailAddress="+document.demoreq.email.value
  }
  //-- If all fields OK go ahead and submit the form and put up a message.
  if (okSoFar==true) {
    //-- The statement below actually submits the form, if all OK.
    document.demoreq.submit()
  }
}
</script>
</div>
<FORM NAME="demoreq" ACTION="/apply/certreq" ENCTYPE=x-www-form-encoded METHOD=POST>
  <div align="center"> 
    <p>PLEASE NOTE! Certificates issued by this CA comes with absolutely<br>
      NO WARRANTY whatsoever. NO AUTHENTICATION is<br>
      performed on the information entered below.</p>
    <p> Please give your username and password, paste the PEM-formatted<br>
      PKCS10 certification request into the field below<br>
      and click OK to fetch your certificate. </p>
  </div>
  <p align="center"> A PEM-formatted request is a BASE64 encoded PKCS10 request 
    between the two lines:<BR>
    -----BEGIN CERTIFICATE REQUEST-----<br>
    -----END CERTIFICATE REQUEST----- 
  <p align="center"> 
    <INPUT name=user type=hidden>
    <br>
    <INPUT name=dn type=hidden value="C=SE,O=AnaTom,CN=">
    <br>
    Full name, e.g. Sven Svensson: 
    <INPUT NAME=cn TYPE=text SIZE=25 maxlength="60" class="input">
    <br>
    E-mail (optional): 
    <INPUT name=email TYPE=text size=25 maxlength="60" class="input">
  
  <p align="center"> 
    <textarea rows="15" cols="70" name=pkcs10req wrap="physical"></textarea>
    <br>
    <br>
    <input type="button" value="OK" onclick="validateForm()">
    <script language="JavaScript">
//-- This little script, executed after form has been rendered, 
  //-- puts the cursor into the userName field so it's ready when page opens.
  document.demoreq.cn.focus()
</script>
</FORM>
<p>&nbsp;</p></BODY>
</HTML>
