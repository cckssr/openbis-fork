import openbis from "@srcV3/openbis.esm";

export async function fetchRights(openbisFacade: openbis.openbis, objId: string, ids: any[]): Promise<{ editable: boolean, deletable: boolean }> {
	const { RightsFetchOptions, getRightsByIds } = openbisFacade;
	const right = await getRightsByIds(ids, new RightsFetchOptions()) as any;
	let editable = false;
	let deletable = false;
	if (right[objId] && right[objId].rights) {
		editable = right[objId].rights.includes("UPDATE");
		deletable = right[objId].rights.includes("DELETE");
	}
	return { editable, deletable };
}

export async function getUserRole(openbisFacade: openbis.openbis, isAdmin: boolean, space: string, project?: string): Promise<string[]> {
	const roles: string[] = [];
	if (isAdmin) {
		roles.push("ADMIN");
	} else {
		const { RoleAssignmentSearchCriteria, RoleAssignmentFetchOptions } = openbisFacade;
		const criteria = new RoleAssignmentSearchCriteria();
		criteria.withSpace().withCode().thatEquals(space);
		//if (form.user) {
		criteria.withOrOperator();
		criteria.withUser().withUserId().thatEquals('admin');
		criteria.withAuthorizationGroup().withUser().withUserId().thatEquals('admin');
		//}
		const fetchOptions = new RoleAssignmentFetchOptions();
		fetchOptions.withSpace();
		fetchOptions.withProject();
		fetchOptions.withUser();
		fetchOptions.withAuthorizationGroup();
		await openbisFacade.searchRoleAssignments(criteria, fetchOptions)
			.then(roleAssignments => {
				var roles = [];
				console.log({ roleAssignments });
				for (let i = 0; i < roleAssignments.getObjects().length; i++) {
					const ra = roleAssignments.getObjects()[i];
					if (ra.space && ra.space.code === space && roles.indexOf(ra.role) < 0) {
						roles.push(ra.role);
					}
					if (project && ra.project && ra.project.code === project && roles.indexOf(ra.role) < 0) {
						roles.push(ra.role);
					}
				}
				roles.push(roles);
			})
			.catch((errorResult: any) => {
				console.error("Error searching role assignments:", errorResult);
				return roles;
			})
	}
	return roles;
}

export async function getRoleAssignments(openbisFacade: openbis.openbis, user: string, space?: string, project?: string): any {
	const { RoleAssignmentSearchCriteria, RoleAssignmentFetchOptions } = openbisFacade;
	const criteria = new RoleAssignmentSearchCriteria();
	criteria.withSpace().withCode().thatEquals(space);
	//if (form.user) {
	criteria.withOrOperator();
	criteria.withUser().withUserId().thatEquals('admin');
	criteria.withAuthorizationGroup().withUser().withUserId().thatEquals('admin');
	//}
	const fetchOptions = new RoleAssignmentFetchOptions();
	fetchOptions.withSpace();
	fetchOptions.withProject();
	fetchOptions.withUser();
	fetchOptions.withAuthorizationGroup();
	const roleAssignments = await openbisFacade.searchRoleAssignments(criteria, fetchOptions)
	const roles = [];
	console.log({ roleAssignments });
	for (let i = 0; i < roleAssignments.length; i++) {
		const ra = roleAssignments[i];
		if (
			ra.space &&
			ra.space.code === space &&
			roles.indexOf(ra.role) < 0
		) {
			roles.push(ra.role);
		}
		if (
			ra.project &&
			project &&
			ra.project.code === project &&
			roles.indexOf(ra.role) < 0
		) {
			roles.push(ra.role);
		}
	}
	return roles;

}
