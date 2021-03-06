<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>

<jsp:include page="header.jsp">
	<jsp:param name="pageTitle" value="PowerAuth 2.0 Admin - New Application"/>
</jsp:include>

	<ol class="breadcrumb">
		<li><a class="black" href="${pageContext.request.contextPath}/application/list">Applications</a></li>
		<li class="active">New application</li>
	</ol>
	
	
	<div class="panel panel-default">

		<div class="panel-heading">
			<h3 class="panel-title">Create a new application</h3>
		</div>
		
		<div class="panel-body">
			<form class="form-inline" action="${pageContext.request.contextPath}/application/create/do.submit" method="POST">
				Application name: <input type="text" name="name" class="form-control"> 
				<input type="submit" value="Submit" class="btn btn-success" />
			</form>
		</div>
		
	</div>
	
<jsp:include page="footer.jsp"/>