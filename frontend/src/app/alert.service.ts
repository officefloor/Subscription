import { Injectable } from '@angular/core';

export interface AlertListener {
    success( message: string )
    error( message: any )
}

@Injectable( {
    providedIn: 'root'
} )
export class AlertService {

    private listeners: AlertListener[] = []

    constructor() { }

    public addListener( listener: AlertListener ) {
        this.listeners.push( listener )
    }

    public removeListener( listener: AlertListener ) {
        const index = this.listeners.indexOf( listener )
        if ( index >= 0 ) {
            this.listeners.splice( index, 1 )
        }
    }

    public success( message: string ) {
        this.listeners.forEach(( listener: AlertListener ) => listener.success( message ) )
    }

    public error( message: any ) {
        this.listeners.forEach(( listener: AlertListener ) => listener.error( message ) )
    }

}