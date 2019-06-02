import { Injectable } from '@angular/core';
import { ServerApiService, Initialisation } from './server-api.service';

@Injectable( {
    providedIn: 'root'
} )
export class InitialiseService {

    private initialisePromise = new Promise(( resolve: ( Initialisation ) => void, reject: ( Error ) => void ) => {
        this.serverApiService.getInitialisation().subscribe( resolve
            , ( error: any ) => {
                console.error( "Initialisation failure", error )
                reject( error )
            } )
    } )

    constructor(
        private serverApiService: ServerApiService
    ) { }

    public intialisation(): Promise<Initialisation> {
        return this.initialisePromise
    }

}