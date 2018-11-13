import { Component, Output, EventEmitter } from '@angular/core';
import { FormGroup, FormBuilder, Validators, FormControl } from '@angular/forms';
import { Router, ActivatedRoute, Params } from '@angular/router';
import { Observable } from 'rxjs/Observable';
import { Location } from '@angular/common';

import { Pathology }    from '../shared/pathology.model';
import { PathologyService } from '../shared/pathology.service';

import { KeycloakService } from "../../../../shared/keycloak/keycloak.service";
import { Mode } from "../../../shared/mode/mode.model";
import { Modes } from "../../../shared/mode/mode.enum";
import { ModesAware } from "../../../shared/mode/mode.decorator";
import { EntityComponent } from '../../../../shared/components/entity/entity.component.abstract';

@Component({
    selector: 'pathology-form',
    templateUrl: 'pathology-form.component.html',
    providers: [PathologyService]
})
@ModesAware
export class PathologyFormComponent extends EntityComponent<Pathology>{

    constructor(
        private route: ActivatedRoute,
        private pathologyService: PathologyService) {

            super(route, 'preclinical-pathology');
    }

    get pathology(): Pathology { return this.entity; }
    set pathology(pathology: Pathology) { this.entity = pathology; }

    initView(): Promise<void> {
        return this.pathologyService.get(this.id).then(pathology => {
            this.pathology = pathology;
        });
    }

    initEdit(): Promise<void> {
        return this.pathologyService.get(this.id).then(pathology => {
            this.pathology = pathology;
        });
    }

    initCreate(): Promise<void> {
        this.entity = new Pathology();
        return Promise.resolve();
    }

    buildForm(): FormGroup {
        return this.formBuilder.group({
            'name': [this.pathology.name, Validators.required]
        });
    }

    
}