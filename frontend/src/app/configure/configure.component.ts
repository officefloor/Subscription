import { Component, OnInit } from '@angular/core'
import { FormGroup, FormControl, FormArray, FormBuilder, AbstractControl } from '@angular/forms'
import { ServerApiService, Configuration, Administrator } from '../server-api.service'
import { SocialUser } from "angularx-social-login"
import { AuthenticationService } from '../authentication.service'
import { AlertService } from '../alert.service'
import { JSON, Error } from 'core-js'
import { of, throwError } from 'rxjs'
import { finalize } from 'rxjs/operators'
import { concatFMap } from '../rxjs.util'


@Component( {
    selector: 'app-configure',
    templateUrl: './configure.component.html',
    styleUrls: ['./configure.component.css']
} )
export class ConfigureComponent implements OnInit {

    isSaving: boolean = false

    configurationForm: FormGroup

    isHide: boolean = true

    constructor(
        private serverApiService: ServerApiService,
        private authenticationService: AuthenticationService,
        private formBuilder: FormBuilder,
        private alertService: AlertService,
    ) { }

    ngOnInit() {

        // Setup form
        this.configurationForm = this.formBuilder.group( {
            googleClientId: '',
            administrators: this.formBuilder.array( [] ),
            paypalEnvironment: '',
            paypalClientId: '',
            paypalClientSecret: '',
            paypalInvoiceIdTemplate: '',
            paypalCurrency: '',
        } )

        // Load form
        this.authenticationService.authenticationState().pipe(
            concatFMap(( user: SocialUser ) => {

                // Only load configuration if logged in user
                if ( !user ) {
                    return throwError( 'Must be logged in to access configuration' )
                }

                // Load configuration
                return this.serverApiService.getConfiguration()
            } )
        ).subscribe(( configuration: Configuration ) => {

            // Function to load values
            this.configurationForm.patchValue( {
                googleClientId: configuration.googleClientId,
                paypalEnvironment: configuration.paypalEnvironment,
                paypalClientId: configuration.paypalClientId,
                paypalClientSecret: configuration.paypalClientSecret,
                paypalInvoiceIdTemplate: configuration.paypalInvoiceIdTemplate,
                paypalCurrency: configuration.paypalCurrency
            } )

            // Load the administrators
            if ( configuration.administrators ) {
                configuration.administrators.forEach(( admin: Administrator ) => {
                    this.addAdministrator( admin.googleId, admin.notes )
                } )
            }

            // Show configuration
            this.isHide = false
        }, this.alertService.handleError() )
    }

    getAdministrators(): AbstractControl[] {
        return ( this.configurationForm.get( 'administrators' ) as FormArray ).controls
    }

    addAdministrator( googleId: string = '', notes: string = '' ) {
        const administrators = this.configurationForm.controls.administrators as FormArray
        administrators.push( this.formBuilder.group( {
            googleId: googleId,
            notes: notes
        } ) )
    }

    removeAdministrator( index: number ) {
        const administrators = this.configurationForm.controls.administrators as FormArray
        administrators.removeAt( index )
    }

    updateConfiguration() {
        this.isSaving = true
        const form = this.configurationForm.value
        this.serverApiService.updateConfiguration( {
            googleClientId: form.googleClientId,
            administrators: form.administrators,
            paypalEnvironment: form.paypalEnvironment,
            paypalClientId: form.paypalClientId,
            paypalClientSecret: form.paypalClientSecret,
            paypalInvoiceIdTemplate: form.paypalInvoiceIdTemplate,
            paypalCurrency: form.paypalCurrency,
        } ).pipe(
            finalize(() => this.isSaving = false ),
        ).subscribe(() => {
            this.alertService.success( 'Successfully updated configuration' )
        }, this.alertService.handleError() )
    }

}