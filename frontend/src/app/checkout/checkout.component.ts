import { Component, OnInit, OnChanges, SimpleChanges, Input } from '@angular/core'
import { InitialiseService } from '../initialise.service'
import { Initialisation } from '../server-api.service'

// Loaded via PayPal script
declare let paypal: any;

@Component( {
    selector: 'app-checkout',
    templateUrl: './checkout.component.html',
    styleUrls: ['./checkout.component.css']
} )
export class CheckoutComponent implements OnInit, OnChanges {

    @Input( 'domain' ) domainName: String

    @Input() isShowReset: boolean = false

    static scriptLoadPromises = {}

    constructor(
        private initialiseService: InitialiseService
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
        this.initialiseService.intialisation().then(( initialisation: Initialisation ) => {

            // Load the configuration
            const paypalClientId = initialisation.paypalClientId
            const paypalCurrency = initialisation.paypalCurrency

            // Load Paypal
            const component = this
            this.loadExternalScript( `https://www.paypal.com/sdk/js?client-id=${paypalClientId}&currency=${paypalCurrency}` ).then(() => {
                paypal.Buttons( {
                    createOrder: function( data, actions ) {
                        // Set up the transaction
                        console.log( 'PayPal create order for domain', component.domainName )
                        return actions.order.create( {
                            purchase_units: [{
                                amount: {
                                    value: '5.00', currency: paypalCurrency
                                }
                            }]
                        } );
                    },
                    onApprove: function( data, actions ) {

                        // TODO call server with orderId
                        console.log( 'Order: ' + data.orderID + " with data:\n\n" + JSON.stringify( data, null, 2 ) + "\n\n" );

                        // Capture the funds from the transaction
                        return actions.order.capture().then( function( details ) {
                            // Show a success message to your buyer
                            console.log( 'Transaction details: ' + JSON.stringify( details, null, 2 ) )
                        } );
                    }
                } ).render( '#paypal-button' )
            } )
        } )
    }

}