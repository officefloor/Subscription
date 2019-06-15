/* 
 * This file can be replaced during build by using the `fileReplacements` array.
 * `ng build --prod` replaces `environment.ts` with `environment.prod.ts`.
 * The list of file replacements can be found in `angular.json`.
 */

import 'zone.js/dist/zone-error' // easier development debugging
import { ServerApiService } from '../app/server-api.service'

export const environment = {
    production: false,

    serverUrl: window.location.href.startsWith( 'http://localhost:4200' ) ? 'http://localhost:8080' : '',

    createOrder: (
        domainName: string,
        isRestartSubscription: boolean,
        paypalCurrency: string,
        serverApiService: ServerApiService,
        data: any,
        actions: any ) => {

        // Indicate order
        console.log( 'Dev PayPal create order for domain', domainName, 'with restart', isRestartSubscription )

        // Set up the transaction
        return actions.order.create( {
            purchase_units: [{
                amount: {
                    value: '5.00', currency: paypalCurrency
                }
            }]
        } ).then(( orderId ) => {

            // Indicate result of creating order
            console.log( 'Dev PayPal created order', orderId )

            return orderId
        } )
    },
    capturePayment: (
        orderId: string,
        serverApiService: ServerApiService,
        data: any,
        actions: any ) => {

        // Indicate order
        console.log( 'Order: ' + orderId + " with data:\n\n" + JSON.stringify( data, null, 2 ) + "\n\n" );

        // Capture the funds from the transaction
        return actions.order.capture().then(( details ) => {

            // Show a success message to your buyer
            console.log( 'Dev PayPal captured payment: ' + JSON.stringify( details, null, 2 ) )

            return details
        } )
    }

}
