import { Component, OnInit } from '@angular/core'
import { FormGroup, FormControl, FormArray, FormBuilder, AbstractControl } from '@angular/forms'
import { ServerApiService, Configuration, Administrator } from '../server-api.service'
import { SocialUser } from "angularx-social-login"
import { AuthenticationService } from '../authentication.service'
import { AlertService } from '../alert.service'

declare let JSON: any

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
    ) {
        this.configurationForm = this.formBuilder.group( {
            googleClientId: '',
            administrators: this.formBuilder.array( [] ),
            paypalEnvironment: '',
            paypalClientId: '',
            paypalClientSecret: '',
            paypalInvoiceIdTemplate: '',
            paypalCurrency: '',
        } )
    }

    ngOnInit() {
        this.authenticationService.authenticationState().subscribe(( user: SocialUser ) => {

            // Only load configuration if logged in user
            if ( !user ) {
                return
            }

            // Load configuration
            this.serverApiService.getConfiguration().subscribe(( configuration: Configuration ) => {

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
                configuration.administrators.forEach(( admin: Administrator ) => {
                    this.addAdministrator( admin.googleId, admin.notes )
                } )

                // Show configuration
                this.isHide = false

            }, this.handleError() )
        } )
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
        // Update the configuration
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
        } ).subscribe(() => {
            this.isSaving = false
            this.alertService.success( 'Successfully updated configuration' )
        }, this.handleError() )
    }

    private handleError(): ( error: any ) => void {
        return ( error ) => {

            // Ensure put into state to display error
            this.isSaving = false

            // Flag to hide
            this.isHide = true

            // Alert regarding failure
            this.alertService.error( error )
        }
    }

}