import { Configuration } from '../app/server-api.service'
import { Observable, BehaviorSubject } from 'rxjs'
import { ServerApiService } from '../app/server-api.service'
import 'zone.js/dist/zone-error' // easier development debugging

// This file can be replaced during build by using the `fileReplacements` array.
// `ng build --prod` replaces `environment.ts` with `environment.prod.ts`.
// The list of file replacements can be found in `angular.json`.

export const environment = {
    production: false,
    serverUrl: window.location.href.startsWith( 'http://localhost:4200' ) ? 'http://localhost:8080' : ''
}

// Load default configuration for development
export function getDefaultConfiguration( serverApiService: ServerApiService ): Observable<Configuration> {

    // Initiate the default configuration
    const defaultConfiguration = new BehaviorSubject<Configuration>( {
        paypalEnvironment: '',
        paypalClientId: '',
        paypalClientSecret: '',
    } )

    // Attempt to load default configuration
    serverApiService.getDefaultConfiguration().subscribe(( configuration: Configuration ) => {

        // Update the default configuration
        defaultConfiguration.next( configuration )

    }, ( error: any ) => {
        // Log failure and just use no configuration
        console.warn( 'Failed to load default configuration', error )
    } )

    // Return the default configuration
    return defaultConfiguration
}
