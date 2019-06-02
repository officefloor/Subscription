import { Configuration } from '../app/server-api.service'
import { Observable, BehaviorSubject } from 'rxjs';

export const environment = {
    production: true,
    serverUrl: ''
}

// No defaults for production
export function getDefaultConfiguration( serverApiService: any ): Observable<Configuration> {
    return new BehaviorSubject<Configuration>( {
        paypalEnvironment: '',
        paypalClientId: '',
        paypalClientSecret: '',
    } )
}