import { Injectable } from '@angular/core'
import { ServerApiService, Initialisation } from './server-api.service'
import { AlertService } from './alert.service'
import { Observable, BehaviorSubject } from 'rxjs'
import { filter } from 'rxjs/operators'

@Injectable( {
    providedIn: 'root'
} )
export class InitialiseService {

    private initialiseState = new BehaviorSubject<Initialisation>( null )

    constructor(
        private serverApiService: ServerApiService,
        private alertService: AlertService,
    ) {
        // Attempt to load initialisation
        this.serverApiService.getInitialisation().subscribe(
            ( init ) => this.initialiseState.next( init ),
            ( error ) => {
                this.alertService.error( error )
                this.initialiseState.error( error )
            }
        )
    }

    public initialisation(): Observable<Initialisation> {
        return this.initialiseState.pipe(
            filter(( init ) => init !== null )
        )
    }

}