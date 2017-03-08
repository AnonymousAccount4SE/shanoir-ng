import { Component, ViewChild, ViewContainerRef } from '@angular/core';
import { MdDialog, MdDialogConfig, MdDialogRef } from '@angular/material';

import { ConfirmDialogComponent } from "../../shared/utils/confirm.dialog.component";
import { ConfirmDialogService } from "../../shared/utils/confirm.dialog.service";
import { TableComponent } from "../../shared/table/table.component";
import { Center } from '../shared/center.model';
import { CenterService } from '../shared/center.service';

@Component({
    selector: 'center-list',
    templateUrl: 'center.list.component.html',
    styleUrls: ['center.list.component.css']
})

export class CenterListComponent {
    public centers: Center[];
    public columnDefs: any[];
    public customActionDefs: any[];
    public rowClickAction: Object;
    public loading: boolean = false;

    constructor(private centerService: CenterService, private confirmDialogService: ConfirmDialogService, private viewContainerRef: ViewContainerRef) {
        this.getCenters();
        this.createColumnDefs();
    }   

    // Grid data
    getCenters(): void {
        this.loading = true;
        this.centerService.getCenters().then(centers => {
            if (centers) {
                this.centers = centers;
            }
            this.loading = false;
        })
        .catch((error) => {
            // TODO: display error
            this.centers = [];
        });
    }

    // Grid columns definition
    private createColumnDefs() {
        function dateRenderer(date: number) {
            if (date) {
                return new Date(date).toLocaleDateString();
            }
            return null;
        };
        this.columnDefs = [
            {headerName: "Name", field: "name" },
            {headerName: "Town", field: "city" },
            {headerName: "Country", field: "country" },
            {headerName: "", type: "button", img: "/assets/images/icons/garbage-1.png", action: this.openDeleteCenterConfirmDialog},
            {headerName: "", type: "button", img: "/assets/images/icons/edit.png", target : "/editCenter", getParams: function(item: any): Object {
                return {id: item.id};
            }},
            {headerName: "", type: "button", img: "/assets/images/icons/view-1.png", target : "/viewCenter", getParams: function(item: any): Object {
                return {id: item.id};
            }}
        ];
        this.customActionDefs = [
            {title: "new center", img: "/assets/images/icons/add-1.png", target: "../editCenter"},
            {title: "delete selected", img: "/assets/images/icons/garbage-1.png", action: this.deleteAll } 
        ];
        this.rowClickAction = {target : "/viewCenter", getParams: function(item: any): Object {
                return {id: item.id};
        }};
    }

    openDeleteCenterConfirmDialog = (item: Center) => {
         this.confirmDialogService
                .confirm('Delete center', 'Are you sure you want to delete center ' + item.name + ' , ' + item.city + ' , ' + item.country + '?',
                    this.viewContainerRef)
                .subscribe(res => {
                    if (res) {
                        this.deleteCenter(item.id);
                    }
                })
    }

    deleteCenter(centerId: number) {
        // Delete center and refresh page
        this.centerService.delete(centerId).then((res) => this.getCenters());
    }

    deleteAll = () => {
        let ids: number[] = [];
        for (let center of this.centers) {
            if (center["isSelectedInTable"]) ids.push(center.id);
        }
        if (ids.length > 0) {
            console.log("TODO : delete those ids : " + ids);
        }
    }

}