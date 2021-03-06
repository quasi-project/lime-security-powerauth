<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>

<jsp:include page="header.jsp">
	<jsp:param name="pageTitle" value="PowerAuth 2.0 Admin - New Application Version"/>
</jsp:include>

	<ol class="breadcrumb">
		<li><a class="black" href="${pageContext.request.contextPath}/application/list">Applications</a></li>
		<li><a class="black" href="${pageContext.request.contextPath}/application/detail/<c:out value="${applicationId}"/>">Application detail</a></li>
		<li class="active">New version</li>
	</ol>
	
	
	<div class="panel panel-default">

		<div class="panel-heading">
			<h3 class="panel-title">Create a new application version</h3>
		</div>
		
		<div class="panel-body">
			<form class="form-inline" action="${pageContext.request.contextPath}/application/detail/<c:out value="${applicationId}"/>/version/create/do.submit" method="POST">
				Version name: <input type="text" name="name" class="form-control"/>
				<input type="submit" value="Submit" class="btn btn-success" />
			</form>
		</div>
		
	</div>
	
<jsp:include page="footer.jsp"/>