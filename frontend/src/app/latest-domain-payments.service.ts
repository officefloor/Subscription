import { Injectable } from '@angular/core'
import { DomainPayments } from './server-api.service'

export interface DomainPaymentsListener {
    latestDomainPayments( domainPayments: DomainPayments )
}

@Injectable( {
    providedIn: 'root'
} )
export class LatestDomainPaymentsService {

    private listeners: DomainPaymentsListener[] = []

    constructor() { }

    public addListener( listener: DomainPaymentsListener ) {
        this.listeners.push( listener )
    }

    public removeListener( listener: DomainPaymentsListener ) {
        const index = this.listeners.indexOf( listener )
        if ( index >= 0 ) {
            this.listeners.splice( index, 1 )
        }
    }

    public notifyLatest( domainPayments: DomainPayments ) {
        this.listeners.forEach(( listener: DomainPaymentsListener ) => listener.latestDomainPayments( domainPayments ) )
    }
}