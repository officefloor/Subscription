/* 
 * This file can be replaced during build by using the `fileReplacements` array.
 * `ng build --prod` replaces `environment.ts` with `environment.prod.ts`.
 * The list of file replacements can be found in `angular.json`.
 */

import 'zone.js/dist/zone-error' // easier development debugging
import { ServerApiService, DomainPayments, Subscription, formatDate } from '../app/server-api.service'
import * as moment from 'moment'

export const environment = {
    production: false,

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

            // Return the updated payment
            const now = moment()
            const subscription: Subscription = {
                paymentDate: formatDate( now ),
                extendsToDate: formatDate( now.add( 1, 'year' ) ),
                isRestartSubscription: false,
                paidByName: 'Testing',
                paidByEmail: 'test@test.com',
                paymentOrderId: orderId,
                paymentReceipt: 'testing',
                paymentAmount: 500
            }
            const domainPayments: DomainPayments = {
                domainName: 'paid.domain',
                expiresDate: formatDate( now.add( 1, 'year' ) ),
                payments: [subscription]
            }
            return domainPayments
        } )
    }

}
