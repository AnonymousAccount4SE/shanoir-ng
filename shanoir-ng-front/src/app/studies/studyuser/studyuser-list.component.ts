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
import { Component, forwardRef, Input, OnChanges, SimpleChanges, ViewChild } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Center } from '../../centers/shared/center.model';
import { preventInitialChildAnimations, slideDown } from '../../shared/animations/animations';

import { Mode } from '../../shared/components/entity/entity.component.abstract';
import { BrowserPaging } from '../../shared/components/table/browser-paging.model';
import { FilterablePageable, Page } from '../../shared/components/table/pageable.model';
import { TableComponent } from '../../shared/components/table/table.component';
import { ColumnDefition } from '../../shared/components/table/column.definition.type';
import { KeycloakService } from '../../shared/keycloak/keycloak.service';
import { Option } from '../../shared/select/select.component';
import { User } from '../../users/shared/user.model';
import { capitalsAndUnderscoresToDisplayable } from '../../utils/app.utils';
import { StudyCenter } from '../shared/study-center.model';
import { StudyUserRight } from '../shared/study-user-right.enum';
import { StudyUser } from '../shared/study-user.model';
import { Study } from '../shared/study.model';

@Component({
    selector: 'studyuser-list',
    templateUrl: 'studyuser-list.component.html',
    styleUrls: ['studyuser-list.component.css'],
    providers: [
        {
          provide: NG_VALUE_ACCESSOR,
          useExisting: forwardRef(() => StudyUserListComponent),
          multi: true,
        }] 
})

export class StudyUserListComponent implements ControlValueAccessor, OnChanges {

    studyUserList: StudyUser[] = [];
    @Input() mode: Mode;
    @Input() users: User[] = [];
    userOptions: Option<User>[];
    @Input() studies: Study[] = [];
    @Input() studyCenters: StudyCenter[] = [];
    centers: Center[] = [];
    studyOptions: Option<Study>[];
    private browserPaging: BrowserPaging<StudyUser>;
    columnDefs: ColumnDefition[];
    @ViewChild('memberTable', { static: false }) table: TableComponent;
    private freshlyAddedMe: boolean = false;
    private studyUserBackup: StudyUser[] = [];
    pannelStudyUser: StudyUser;
    StudyUserRight = StudyUserRight;
    isAdmin: boolean;

    private onTouchedCallback = () => {};
    private onChangeCallback = (_: any) => {};

    constructor(private keycloakService: KeycloakService) {
        this.isAdmin = keycloakService.isUserAdmin();
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.users && this.users) {
            this.userOptions = this.users.map(user => {
                let option: Option<User> = new Option<User>(user, user.username + '(' + user.firstName + ' ' + user.lastName + ')');
                option.disabled = !!this.studyUserList?.find(su => su.userId == user.id || su.user?.id == user.id);
                return option;
            });
        }
        if (changes.studies && this.studies) {
            this.studyOptions = this.studies.map(study => {
                let option: Option<Study> = new Option<Study>(study, study.name);
                option.disabled = !!this.studyUserList?.find(su => su.studyId == study.id || su.study?.id == study.id);
                return option;
            });
        }
        if (changes.mode) {
            this.createColumnDefs(this.mode != 'view');
        }
        if (changes.studyCenters) {
            this.centers = this.studyCenters?.map(sc => sc.center);
            this.studyUserList?.forEach(su => {
                su.centers = su.centers?.filter(suc => this.centers.findIndex(c => c.id == suc.id) > -1);
            });
        }
    }

    writeValue(obj: any): void {
        this.studyUserList = obj ? obj : [];
        this.studyUserList?.forEach(su => {
            su.centers = su.centers?.filter(suc => this.centers.findIndex(c => c.id == suc.id) > -1);
        });
        this.studyUserBackup = [...this.studyUserList];
        this.browserPaging = new BrowserPaging(this.studyUserList, this.columnDefs);
        this.getPage = (pageable) => Promise.resolve(this.browserPaging.getPage(pageable));
    }

    registerOnChange(fn: any): void {
        this.onChangeCallback = fn;
    }

    registerOnTouched(fn: any): void {
        this.onTouchedCallback = fn;
    }

    getPage: (FilterablePageable) => Promise<Page<StudyUser>> = () => Promise.resolve(new Page());

    isMe(user: User): boolean {
        return user.id == KeycloakService.auth.userId;
    }

    disableEdit(studyUser: StudyUser): boolean {
        return !this.freshlyAddedMe && studyUser.userId == KeycloakService.auth.userId;
    }

    private createColumnDefs(deleteButton: boolean) {
        this.columnDefs = [
            { headerName: 'Username', field: 'userName' },
            // { headerName: 'First Name', field: 'user.firstName' },
            // { headerName: 'Last Name', field: 'user.lastName' },
            // { headerName: 'Email', field: 'user.email', width: '200%' },
            { headerName: 'Role', field: 'user.role.displayName', width: '80px', defaultSortCol: true },
            { headerName: 'Confirmed', field: 'confirmed', type: 'boolean', editable: false, width: '54px', disableSorting: true},
            // { headerName: 'Centers', type: 'boolean', editable: false, width: '54px', disableSorting: true,
            //     cellRenderer: (params: any) => !params.data.centers || params.data.centers.length == 0},
            { headerName: 'Can see all', type: 'boolean', editable: false, width: '54px', disableSorting: true,
                //onEdit: (su: StudyUser, value: boolean) => this.onEditRight(StudyUserRight.CAN_SEE_ALL, su, value),
                cellRenderer: (params: any) => params.data.studyUserRights.includes(StudyUserRight.CAN_SEE_ALL)},
            { headerName: 'Can download', type: 'boolean', editable: (su: StudyUser) => !this.disableEdit(su), width: '54px', disableSorting: true, 
                onEdit: (su: StudyUser, value: boolean) => this.onEditRight(StudyUserRight.CAN_DOWNLOAD, su, value),
                cellRenderer: (params: any) => params.data.studyUserRights.includes(StudyUserRight.CAN_DOWNLOAD)},
            { headerName: 'Can import', type: 'boolean', editable: (su: StudyUser) => !this.disableEdit(su), width: '54px', disableSorting: true, 
                onEdit: (su: StudyUser, value: boolean) => this.onEditRight(StudyUserRight.CAN_IMPORT, su, value),
                cellRenderer: (params: any) => params.data.studyUserRights.includes(StudyUserRight.CAN_IMPORT)},
            { headerName: 'Can admin', type: 'boolean',  disableSorting: true, editable: (su: StudyUser) => su.user && su.user.role.displayName != 'User' && !this.disableEdit(su), width: '54px', 
                onEdit: (su: StudyUser, value: boolean) => this.onEditRight(StudyUserRight.CAN_ADMINISTRATE, su, value),
                cellRenderer: (params: any) => params.data.studyUserRights.includes(StudyUserRight.CAN_ADMINISTRATE), },
            // { headerName: 'Receive Import Mail', type: 'boolean', field: 'receiveNewImportReport', editable: true, width: '54px' },
            // { headerName: 'Receive Member Mail', type: 'boolean', field: 'receiveStudyUserReport', editable: true, width: '54px' },
        ];
        if (deleteButton) {
            this.columnDefs.push({ headerName: '', type: 'button', awesome: 'fa-regular fa-trash-can', action: this.removeStudyUser, editable: (su: StudyUser) => !this.disableEdit(su)});
        }
    }

    /**
     * On select/unselect given right for the given study user 
     */
    onEditRight(right: StudyUserRight, su: StudyUser, selected: boolean) {
        if (!su.studyUserRights.includes(right) && selected) {
            su.studyUserRights.push(right);
        }
        else if (su.studyUserRights.includes(right) && !selected) {
            const index = su.studyUserRights.indexOf(right, 0);
            if (index > -1) su.studyUserRights.splice(index, 1);
        }
        this.onStudyUserEdit();
    }

    onEditCenter(center: Center, su: StudyUser, selected: boolean) {
        if (!su.centers) su.centers = [];
        let index: number = su.centers.findIndex(c => c.id == center.id)
        if (!su.centers.find(c => c.id == center.id) && selected) {
            su.centers.push(center);
        }
        else if (index > -1 && !selected) {
            su.centers.splice(index, 1);
        }
        this.onStudyUserEdit();
    }

    hasCenter(center: Center, su: StudyUser): boolean {
        return !!su.centers?.find(c => c.id == center.id);
    }

    onUserClick(studyUser: StudyUser) {
        if (this.pannelStudyUser && (this.pannelStudyUser.id == studyUser.id)) {
            this.closePannel();
        } else {
            this.openPannel(studyUser);
        }
    }

    openPannel(studyUser: StudyUser) {
        this.pannelStudyUser = studyUser;
    }

    closePannel() {
        this.pannelStudyUser = null;
    }

    private removeStudyUser = (item: StudyUser) => {
        const index: number = this.studyUserList.indexOf(item);
        if (index !== -1) {
            this.studyUserList.splice(index, 1);
        }
        this.browserPaging.setItems(this.studyUserList);
        this.table.refresh();
        this.onChangeCallback(this.studyUserList);
        this.onTouchedCallback();
        StudyUser.completeMember(item, this.users);
        if (this.userOptions) {
            let option = this.userOptions.find(opt => opt.value?.id == item.user?.id);
            if (option) option.disabled = false;
        }
        this.closePannel();
    }

    onStudyUserEdit() {
        this.onChangeCallback(this.studyUserList);
        this.onTouchedCallback();
    }

    studyStatusStr(studyStatus: string) {
        return capitalsAndUnderscoresToDisplayable(studyStatus);
    }

    onUserAdd(selectedUser: User) {
        if (!selectedUser) {
            return;
        }
        if (this.studyUserList.filter(user => user.userId == selectedUser.id).length > 0){
            return;   
        }
        if (this.isMe(selectedUser)) {
            this.freshlyAddedMe = true;
        }
        this.addUser(selectedUser);
    }

    addUser(selectedUser: User, rights: StudyUserRight[] = [StudyUserRight.CAN_SEE_ALL]) {
        if (this.userOptions) {
            let option = this.userOptions.find(opt => opt.value.id == selectedUser.id);
            if (option) option.disabled = true;
        }

        let backedUpStudyUser: StudyUser = this.studyUserBackup.filter(su => su.userId == selectedUser.id)[0];
        if (backedUpStudyUser) {
            this.studyUserList.unshift(backedUpStudyUser);
            this.pannelStudyUser = backedUpStudyUser;
        } else {
            let studyUser: StudyUser = new StudyUser();
            studyUser.userId = selectedUser.id;
            studyUser.userName = selectedUser.username;
            studyUser.receiveStudyUserReport = false;
            studyUser.receiveNewImportReport = false;
            studyUser.studyUserRights = rights;
            studyUser.completeMember(this.users);
            this.studyUserList.unshift(studyUser);
            this.pannelStudyUser = studyUser;
        }
        this.browserPaging.setItems(this.studyUserList);
        this.table.refresh();
        this.onChangeCallback(this.studyUserList);
        this.onTouchedCallback();
    }

    onStudyAdd(selectedStudy: Study) {
        if (!selectedStudy) {
            return;
        }
        if (!this.studyUserList.find(su => su.study?.id == selectedStudy.id)){
            return;   
        }
        this.addStudy(selectedStudy);
    }

    addStudy(selectedStudy: Study, rights: StudyUserRight[] = [StudyUserRight.CAN_SEE_ALL]) {
        if (this.studyOptions) {
            let option = this.studyOptions.find(opt => opt.value.id == selectedStudy.id);
            if (option) option.disabled = true;
        }

        let backedUpStudyUser: StudyUser = this.studyUserBackup.find(su => su.study?.id == selectedStudy.id);
        if (backedUpStudyUser) {
            this.studyUserList.unshift(backedUpStudyUser);
        } else {
            let studyUser: StudyUser = new StudyUser();
            studyUser.study = selectedStudy;
            studyUser.receiveStudyUserReport = false;
            studyUser.receiveNewImportReport = false;
            studyUser.studyUserRights = rights;
            studyUser.completeMember(this.users);
            this.studyUserList.unshift(studyUser);
        }
        this.browserPaging.setItems(this.studyUserList);
        this.table.refresh();
        this.onChangeCallback(this.studyUserList);
        this.onTouchedCallback();
    }

    onToggleAllCenters(check: boolean) {
        if(!check) {
            this.pannelStudyUser.centers = [...this.centers];
        } else {
            this.pannelStudyUser.centers = [];
        }
    }
}
