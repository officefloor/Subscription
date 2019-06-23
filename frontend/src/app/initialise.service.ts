import { Injectable } from '@angular/core'
import { ServerApiService, Initialisation } from './server-api.service'
import { Promise, Error } from 'core-js'
import { AlertService } from './alert.service'
import { Observable, from } from 'rxjs'
import { isEmpty } from 'rxjs/operators'

@Injectable( {
    providedIn: 'root'
} )
export class InitialiseService {

    private initialisePromise = new Promise<Initialisation>(( resolve, reject ) =>
        this.serverApiService.getInitialisation().pipe(
            this.alertService.alertError(( error ) => {
                this.alertService.error( "Failed to initialise. Please refresh page." )
                return true
            } )
        ).subscribe( resolve, reject )
    )

    constructor(
        private serverApiService: ServerApiService,
        private alertService: AlertService,
    ) { }

    public initialisation(): Observable<Initialisation> {
        return from( this.initialisePromise ) as Observable<Initialisation>
    }

}