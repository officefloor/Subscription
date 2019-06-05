import { Component, OnInit } from '@angular/core'
import { FormGroup, FormControl } from '@angular/forms'
import { ServerApiService, Configuration } from '../server-api.service'
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

    configurationForm = new FormGroup( {
        paypalEnvironment: new FormControl( '' ),
        paypalClientId: new FormControl( '' ),
        paypalClientSecret: new FormControl( '' )
    } )

    errorMessage: string = null

    constructor(
        private serverApiService: ServerApiService,
        private authenticationService: AuthenticationService,
    ) { }

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
                    paypalEnvironment: configuration.paypalEnvironment,
                    paypalClientId: configuration.paypalClientId,
                    paypalClientSecret: configuration.paypalClientSecret,
                } )

            }, this.handleError() )
        } )
    }

    updateConfiguration() {
        // Update the configuration
        this.isSaving = true
        const form = this.configurationForm.value
        this.serverApiService.updateConfiguration( {
            paypalEnvironment: form.paypalEnvironment,
            paypalClientId: form.paypalClientId,
            paypalClientSecret: form.paypalClientSecret
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