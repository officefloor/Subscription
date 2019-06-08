import { Component, OnInit } from '@angular/core'
import { FormGroup, FormControl, FormArray, FormBuilder } from '@angular/forms'
import { ServerApiService, Configuration, Administrator } from '../server-api.service'
import { SocialUser } from "angularx-social-login"
import { AuthenticationService } from '../authentication.service'

declare let JSON: any

@Component( {
    selector: 'app-configure',
    templateUrl: './configure.component.html',
    styleUrls: ['./configure.component.css']
} )
export class ConfigureComponent implements OnInit {

    isSaving: boolean = false

    configurationForm: FormGroup

    errorMessage: string = null

    constructor(
        private serverApiService: ServerApiService,
        private authenticationService: AuthenticationService,
        private formBuilder: FormBuilder,
    ) {
        this.configurationForm = this.formBuilder.group( {
            googleClientId: '',
            administrators: this.formBuilder.array( [] ),
            paypalEnvironment: '',
            paypalClientId: '',
            paypalClientSecret: '',
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
                    paypalCurrency: configuration.paypalCurrency
                } )

                // Load the google administrator Ids
                configuration.administrators.forEach(( admin: Administrator ) => {
                    this.addAdministrator( admin.googleId, admin.notes )
                } )

                // TODO REMOVE
                console.log( 'TODO REMOVE form: ' + JSON.stringify( this.configurationForm.value ) )

            }, this.handleError() )
        } )
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
            paypalCurrency: form.paypalCurrency,
        } ).subscribe(() => {
            this.isSaving = false
            console.log( 'Successfully updated configuration' )
        }, this.handleError() )
    }

    private handleError(): ( error: any ) => void {
        return ( error ) => {

            // Ensure put into state to display error
            this.isSaving = false

            // Indicate detail of the error
            console.warn( 'Access error: ', error )

            // Display error
            if ( error['status'] === 401 ) {
                this.errorMessage = 'Login timed out. Please login again'
            } else if ( error['status'] === 403 ) {
                this.errorMessage = 'Sorry, you do not have permissions for configuration'
            } else if ( error['statusText'] ) {
                this.errorMessage = error['statusText']
            } else if ( error['status'] ) {
                this.errorMessage = 'Technical error. HTTP Status ' + error['status']
            } else {
                this.errorMessage = 'Technical error in accessing configuration'
            }
        }
    }

}