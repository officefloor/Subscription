import { Injectable } from '@angular/core'
import { ServerApiService, Initialisation } from './server-api.service'
import { Promise, Error } from 'core-js'
import { AlertService } from './alert.service'

@Injectable( {
    providedIn: 'root'
} )
export class InitialiseService {

    private initialisePromise = new Promise(
        ( resolve: ( Initialisation ) => void, reject: ( Error ) => void ) =>
            this.serverApiService.getInitialisation().pipe(
                this.alertService.alertError()
            ).subscribe( resolve, reject )
    )

    constructor(
        private serverApiService: ServerApiService,
        private alertService: AlertService,
    ) { }

    public initialisation(): Promise<Initialisation> {
        return this.initialisePromise
    }

}