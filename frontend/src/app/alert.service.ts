import { Injectable } from '@angular/core'
import { Observable, OperatorFunction, throwError, ObservedValueOf } from 'rxjs'
import { catchError } from 'rxjs/operators'

export interface AlertListener {
    success( message: string )
    error( message: any )
}

@Injectable( {
    providedIn: 'root'
} )
export class AlertService {

    private listeners: Array<AlertListener> = new Array()

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

    public alertError<T>( filter: ( error: any ) => boolean = null ): OperatorFunction<T, T | Observable<never>> {
        return catchError(( error ) => {

            // Attempt to handle the error
            this.handleError( filter )( error )

            // Propagate the failure
            return throwError( error )
        } )
    }

    public handleError( filter: ( error: any ) => boolean = null ): ( error: any ) => void {
        return ( error ) => {

            // Ensure can determine if include
            if ( !filter ) {
                filter = () => true
            }

            // Notify of error (if include)
            if ( filter( error ) ) {
                this.error( error )
            }
        }
    }

    public success( message: string ) {
        this.listeners.forEach(( listener: AlertListener ) => listener.success( message ) )
    }

    public error( message: any ) {
        this.listeners.forEach(( listener: AlertListener ) => listener.error( message ) )
    }

}