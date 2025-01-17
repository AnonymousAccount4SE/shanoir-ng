/**
 * Shanoir NG - Import, manage and share neuroimaging data
 * Copyright (C) 2009-2019 Inria - https://www.inria.fr/
 * Contact us on https://project.inria.fr/shanoir/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/gpl-3.0.html
 */
import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { Router } from '@angular/router';
import { DatasetAcquisition } from '../../dataset-acquisitions/shared/dataset-acquisition.model';
import { DatasetProcessing } from '../../datasets/shared/dataset-processing.model';
import { Dataset } from '../../datasets/shared/dataset.model';
import { DatasetProcessingType } from '../../enum/dataset-processing-type.enum';
import { ExaminationPipe } from '../../examinations/shared/examination.pipe';

import { ExaminationService } from '../../examinations/shared/examination.service';
import { SubjectExamination } from '../../examinations/shared/subject-examination.model';
import { DatasetAcquisitionNode, DatasetNode, ExaminationNode, ProcessingNode, ReverseStudyNode, UNLOADED } from '../../tree/tree.model';
import { Study } from '../shared/study.model';
import {KeycloakService} from "../../shared/keycloak/keycloak.service";
import {StudyRightsService} from "../shared/study-rights.service";
import {StudyUserRight} from "../shared/study-user-right.enum";

@Component({
    selector: 'reverse-study-node',
    templateUrl: 'reverse-study-node.component.html'
})

export class ReverseStudyNodeComponent implements OnChanges {

    @Input() input: ReverseStudyNode | Study;
    @Input() subjectId: number;
    @Output() nodeInit: EventEmitter<ReverseStudyNode> = new EventEmitter();
    @Output() selectedChange: EventEmitter<ReverseStudyNode> = new EventEmitter();
    node: ReverseStudyNode;
    loading: boolean = false;
    menuOpened: boolean = false;
    studyCardsLoading: boolean = false;
    showDetails: boolean;
    @Input() hasBox: boolean = false;
    detailsPath: string = '/study/details/';
    private canAdmin: boolean = false;

    constructor(
            private router: Router,
            private examinationService: ExaminationService,
            private examPipe: ExaminationPipe,
            private keycloakService: KeycloakService,
            private studyRightsService: StudyRightsService) {
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (!changes['input']) {
            return;
        }

        this.studyRightsService.getMyRightsForStudy(this.input.id).then(rights => {
            this.canAdmin = this.keycloakService.isUserAdmin()
                || (this.keycloakService.isUserExpert() && rights.includes(StudyUserRight.CAN_ADMINISTRATE));

            if (this.input instanceof ReverseStudyNode) {
                this.node = this.input;
            } else {
                this.node = new ReverseStudyNode(
                    this.input.id,
                    this.input.name,
                    [],
                    UNLOADED);
            }
            this.nodeInit.emit(this.node);
            this.showDetails = this.router.url != '/study/details/' + this.node.id;
        });
    }

    loadExaminations() {
        if (this.node.examinations == UNLOADED) {
            this.loading = true;
            this.examinationService.findExaminationsBySubjectAndStudy(this.subjectId, this.node.id)
            .then(examinations => {
                let sortedExaminations = examinations.sort((a: SubjectExamination, b: SubjectExamination) => {
                    return (new Date(a.examinationDate)).getTime() - (new Date(b.examinationDate)).getTime();
                })
                this.node.examinations = [];
                if (sortedExaminations) {
                    sortedExaminations.forEach(exam => {
                        (this.node.examinations as ExaminationNode[]).push(this.mapExamNode(exam));
                    });
                }
                this.loading = false;
                this.node.open = true;
            }).catch(() => {
                this.loading = false;
            });
        }
    }

    private mapExamNode(exam: SubjectExamination): ExaminationNode {
        return new ExaminationNode(
            exam.id,
            this.examPipe.transform(exam),
            exam.datasetAcquisitions ? exam.datasetAcquisitions.map(dsAcq => this.mapAcquisitionNode(dsAcq)) : [],
            exam.extraDataFilePathList,
            this.canAdmin
        );
    }

    private mapAcquisitionNode(dsAcq: DatasetAcquisition): DatasetAcquisitionNode {
        return new DatasetAcquisitionNode(
            dsAcq.id,
            dsAcq.name,
            dsAcq.datasets ? dsAcq.datasets.map(ds => this.mapDatasetNode(ds, false)) : [],
            this.canAdmin
        );
    }

    private mapDatasetNode(dataset: Dataset, processed: boolean): DatasetNode {
        return new DatasetNode(
            dataset.id,
            dataset.name,
            dataset.type,
            dataset.processings ? dataset.processings.map(proc => this.mapProcessingNode(proc)) : [],
            processed,
            this.canAdmin
        );
    }

    private mapProcessingNode(processing: DatasetProcessing): ProcessingNode {
        return new ProcessingNode(
            processing.id,
            DatasetProcessingType.getLabel(processing.datasetProcessingType),
            processing.outputDatasets ? processing.outputDatasets.map(ds => this.mapDatasetNode(ds, true)) : [],
            this.canAdmin
        );
    }
    hasDependency(dependencyArr: any[] | UNLOADED): boolean | 'unknown' {
        if (!dependencyArr) return false;
        else if (dependencyArr == UNLOADED) return 'unknown';
        else return dependencyArr.length > 0;
    }
}
