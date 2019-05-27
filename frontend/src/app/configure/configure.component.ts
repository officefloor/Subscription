import { Component, OnInit } from '@angular/core';
import { FormGroup, FormControl } from '@angular/forms';

declare let JSON: any

@Component( {
    selector: 'app-configure',
    templateUrl: './configure.component.html',
    styleUrls: ['./configure.component.css']
} )
export class ConfigureComponent implements OnInit {

    configurationForm = new FormGroup( {
        paypalEnvironment: new FormControl( 'sandbox' ),
        paypalClientId: new FormControl( 'CLIENT ID' ),
        paypalClientSecret: new FormControl( 'CLIENT SECRET' )
    } )

    constructor() { }

    ngOnInit() {
    }

    updateConfiguration() {
        console.log( 'Saving values: ' + JSON.stringify(this.configurationForm.value) )
    }
}
