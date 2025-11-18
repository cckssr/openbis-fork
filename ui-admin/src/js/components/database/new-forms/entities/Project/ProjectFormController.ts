import { IFormController } from "@src/js/components/database/new-forms/types/IFormController.ts";
import { Form } from "@src/js/components/database/new-forms/types/form.types.ts";
import { ProjectFormModel } from "@src/js/components/database/new-forms/entities/Project/ProjectFormModel.ts";
import { FormMode } from "@src/js/components/database/new-forms/types/form.enums.ts";

export class ProjectFormController implements IFormController {
	private openbisFacade: any;

	constructor(openbisFacade: any) {
		if (!openbisFacade) throw new Error('openbisFacade is required');
		this.openbisFacade = openbisFacade;
	}

	async load(permId: string): Promise<Form> {
		const { ProjectPermId, ProjectFetchOptions, ExperimentIdentifier, RightsFetchOptions } = this.openbisFacade;
		const id = new ProjectPermId(permId);
		const fetchOptions = new ProjectFetchOptions();
		fetchOptions.withSpace();

		const result = await this.openbisFacade.getProjects([id], fetchOptions);

		const projectDto = result[permId];
		console.log({ projectDto });
		if (!projectDto) throw new Error(`Project with permId ${permId} not found`);
		/* const spaceCode = projectDto.getSpace().getCode();
		const projectCode = projectDto.getCode();
	    
		const sessionInfo = await this.openbisFacade.getSessionInformation();
		console.log({ sessionInfo })
		console.log({ spaceCode }, { projectCode });
		const roles = await getUserRole(this.openbisFacade, false, spaceCode, projectCode);
		console.log({roles}); */
		return ProjectFormModel.adaptProjectDtoToForm(projectDto);
	}

	async save(form: Form, mode: FormMode): Promise<any> {
		
		return null;
	}

	async checkPermissions(form: Form): Promise<Record<'canEdit' | 'canDelete' | 'canMove', boolean>> {
		return {
			canEdit: true,
			canDelete: true,
			canMove: true,
		};
	}

	async delete(form: Form): Promise<void> {
		return;
	}

	async getDependentEntities(form: Form): Promise<any> {
		return null;
	}

	async move(form: Form, context?: any): Promise<void> {
		return;
	}
}