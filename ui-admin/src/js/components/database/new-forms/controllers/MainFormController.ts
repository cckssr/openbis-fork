import openbis from "@srcV3/openbis.esm";
// MainController.ts (or similar service file)
export class AuthorizationService { // Or part of a larger MainController class

	async fetchRights(openbis, objId, ids) {
		const right = await openbis.getRightsByIds(ids, new openbis.RightsFetchOptions())

		if (right[objId] && right[objId].rights) {
			const editable = right[objId].rights.includes("UPDATE")
		}

	}

	async getUserRole(openbisFacade: openbis.openbis, isAdmin: boolean, space: string, project?: string): string[] {
		if (isAdmin) {
			return ["ADMIN"];
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
			const roles = await openbisFacade.searchRoleAssignments(criteria, fetchOptions)
				.then(roleAssignments => {
					var roles = [];
					console.log({ roleAssignments });
					for (let i = 0; i < roleAssignments.length; i++) {
						const ra = roleAssignments[i];
						if (ra.space && ra.space.code === space && roles.indexOf(ra.role) < 0) {
							roles.push(ra.role);
						}
						if (project && ra.project && ra.project.code === project && roles.indexOf(ra.role) < 0) {
							roles.push(ra.role);
						}
					}
					return roles;
				})
				.catch((errorResult: any) => {
					console.error("Error searching role assignments:", errorResult);
					return [];
				})
			return roles;
		}
	}

	async getRoleAssignments(openbisFacade: openbis.openbis, user: string, space?: string, project?: string): any {
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
}