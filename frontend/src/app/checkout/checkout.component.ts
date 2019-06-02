import { Component, OnChanges, SimpleChanges, AfterViewInit, Input } from '@angular/core'
import { InitialiseService } from '../initialise.service'
import { Initialisation } from '../server-api.service'

// Loaded via PayPal script
declare let paypal: any;


@Component( {
    selector: 'app-checkout',
    templateUrl: './checkout.component.html',
    styleUrls: ['./checkout.component.css']
} )
export class CheckoutComponent implements OnChanges, AfterViewInit {

    @Input( 'domain' ) domainName: String

    constructor(
        private initialiseService: InitialiseService
    ) { }

    ngOnChanges( changes: SimpleChanges ) {
        console.log( 'TODO CHANGE: ', this.domainName )
    }

    private loadExternalScript( scriptUrl: string ) {
        return new Promise(( resolve, reject ) => {
            const scriptElement = document.createElement( 'script' )
            scriptElement.src = scriptUrl
            scriptElement.onload = resolve
            document.body.appendChild( scriptElement )
        } )
    }

    ngAfterViewInit(): void {
        this.initialiseService.intialisation().then(( initialisation: Initialisation ) => {


            // TODO load configuration
            const CLIENT_ID = initialisation.paypalClientId
            const ENVIRONMENT = 'sandbox'
            const CURRENCY = 'AUD'

            // Load Paypal
            this.loadExternalScript( `https://www.paypal.com/sdk/js?client-id=${CLIENT_ID}&currency=${CURRENCY}` ).then(() => {
                paypal.Buttons( {
                    createOrder: function( data, actions ) {
                        // Set up the transaction
                        return actions.order.create( {
                            purchase_units: [{
                                amount: {
                                    value: '5.00', currency: CURRENCY
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
