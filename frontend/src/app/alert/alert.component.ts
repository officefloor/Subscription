import { Component, OnInit, OnDestroy } from '@angular/core'
import { AlertService, AlertListener } from '../alert.service'

export class Alert {
    constructor(
        public isError: boolean,
        public message: string,
        private alerts: Alert[],
    ) {
        if ( !this.isError ) {
            setTimeout(() => this.remove(), 5_000)
        }
    }

    remove(): void {
        const index = this.alerts.indexOf( this )
        if ( index >= 0 ) {
            this.alerts.splice( index, 1 )
        }
    }
}

@Component( {
    selector: 'app-alert',
    templateUrl: './alert.component.html',
    styleUrls: ['./alert.component.css']
} )
export class AlertComponent implements OnInit, OnDestroy, AlertListener {

    alerts: Alert[] = []

    constructor(
        private alertService: AlertService
    ) { }

    ngOnInit(): void {
        this.alertService.addListener( this )
    }

    ngOnDestroy(): void {
        this.alertService.removeListener( this )
    }

    success( message: string ) {
        this.addAlert( false, message )
    }
    error( error: any ) {
        console.error( 'Alert', error )
        const technicalErrorMessage = 'Technical failure.  Please reload page and retry. If error continues please raise support ticket'

        // Handle error
        if ( typeof ( error ) === 'string' ) {
            this.addAlert( true, error )

        } else if ( ( error.name === 'HttpErrorResponse' ) && error.status ) {
            // Handle error with server
            switch ( error.status ) {
                case 401:
                    this.addAlert( true, 'Session expired. Please logout and log back in.' )
                    return
                case 403:
                    this.addAlert( true, 'Sorry you do not have permissions' )
                    return
            }
            if ( String(error.status).startsWith( '4' ) ) {
                this.addAlert( true, 'Failure communicating to server' )
            } else {
                this.addAlert( true, technicalErrorMessage )
            }

        } else if ( error.message ) {
            this.addAlert( true, error.message )

        } else {
            this.addAlert( true, technicalErrorMessage )
        }
    }

    private addAlert( isError: boolean, message: string ) {
        this.alerts.push( new Alert( isError, message, this.alerts ) )
    }
}