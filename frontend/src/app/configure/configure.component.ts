import { Component, OnInit } from '@angular/core';
import { FormGroup, FormControl } from '@angular/forms';
import { ServerApiService, Configuration } from '../server-api.service';
import { AuthenticationService } from '../authentication.service';
import { getDefaultConfiguration } from '../../environments/environment';

declare let JSON: any

@Component( {
    selector: 'app-configure',
    templateUrl: './configure.component.html',
    styleUrls: ['./configure.component.css']
} )
export class ConfigureComponent implements OnInit {

    isInitialised: boolean = false

    isSaving: boolean = false

    configurationForm = new FormGroup( {
        paypalEnvironment: new FormControl( '' ),
        paypalClientId: new FormControl( '' ),
        paypalClientSecret: new FormControl( '' )
    } )

    errorMessage: string = null

    constructor(
        private authenticationService: AuthenticationService,
        private serverApiService: ServerApiService
    ) { }

    ngOnInit() {
        // Load configuration (on login/logout)
        this.authenticationService.authenticationState().subscribe(( user: any ) => {

            // Only load if logged in
            if ( !user ) {
                return
            }

            // Load the configuration
            this.serverApiService.getConfiguration().subscribe(( configuration: Configuration ) => {
                try {

                    // Function to load values
                    const setFormValues = ( config: Configuration ) => {
                        this.configurationForm.patchValue( {
                            paypalEnvironment: config.paypalEnvironment,
                            paypalClientId: config.paypalClientId,
                            paypalClientSecret: config.paypalClientSecret,
                        } )
                    }

                    // Determine if have saved values
                    if ( configuration.paypalEnvironment ) {
                        setFormValues( configuration )

                    } else {
                        // Use configured defaults
                        getDefaultConfiguration( this.serverApiService ).subscribe(( defaults: Configuration ) => {
                            setFormValues( defaults )
                        } )
                    }

                } finally {
                    // Flag now able to view form
                    this.isInitialised = true
                }
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
            this.isInitialised = true
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