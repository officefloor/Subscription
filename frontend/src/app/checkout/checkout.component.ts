import { Component, OnInit, SimpleChanges, Input } from '@angular/core'
import { InitialiseService } from '../initialise.service'
import { ServerApiService, Initialisation, DomainPayments } from '../server-api.service'
import { environment } from '../../environments/environment'
import { LatestDomainPaymentsService } from '../latest-domain-payments.service'
import { AlertService } from '../alert.service'
import { Promise } from 'core-js'
import { map } from 'rxjs/operators'

// Loaded via PayPal script
declare let paypal: any;

@Component( {
    selector: 'app-checkout',
    templateUrl: './checkout.component.html',
    styleUrls: ['./checkout.component.css']
} )
export class CheckoutComponent implements OnInit {

    @Input( 'domain' ) domainName: string

    @Input() isShowReset: boolean = false

    isResetSubscription: boolean = false

    static scriptLoadPromises = {}

    constructor(
        private initialiseService: InitialiseService,
        private serverApiService: ServerApiService,
        private latestDomainPaymentsService: LatestDomainPaymentsService,
        private alertService: AlertService,
    ) { }

    private loadExternalScript( scriptUrl: string ) {

        // Determine if already loaded
        let scriptLoadPromise = CheckoutComponent.scriptLoadPromises[scriptUrl]
        if ( scriptLoadPromise ) {
            return scriptLoadPromise
        }

        // Load the script
        scriptLoadPromise = new Promise(( resolve, reject ) => {
            const scriptElement = document.createElement( 'script' )
            scriptElement.src = scriptUrl
            scriptElement.onload = resolve
            document.body.appendChild( scriptElement )
        } )
        CheckoutComponent.scriptLoadPromises[scriptUrl] = scriptLoadPromise

        // Return promise on loading script
        return scriptLoadPromise
    }

    ngOnInit() {
        // Load PayPal for domain
        this.initialiseService.initialisation().pipe(
            map(( initialisation: Initialisation ) => {

                // Load the configuration
                const paypalClientId = initialisation.paypalClientId
                const paypalCurrency = initialisation.paypalCurrency

                // Load Paypal
                const component = this
                this.loadExternalScript( `https://www.paypal.com/sdk/js?client-id=${paypalClientId}&currency=${paypalCurrency}` ).then(() => {
                    paypal.Buttons( {
                        createOrder: ( data, actions ) => environment.createOrder(
                            component.domainName, component.isResetSubscription, paypalCurrency, this.serverApiService,
                            data, actions ),
                        onApprove: ( data, actions ) => environment.capturePayment(
                            data.orderID, this.serverApiService, data, actions ).then(( domainPayments: DomainPayments ) => {
                                this.latestDomainPaymentsService.notifyLatest( domainPayments )
                            } ).catch(( error: any ) => {
                                this.alertService.error( error )
                            } )
                    } ).render( '#paypal-button' )
                } )
            } ) )
    }

}