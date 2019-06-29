import { ServerApiService, CreatedInvoice, DomainPayments } from '../app/server-api.service'

export const environment = {
    production: true,
    isLogHttp: false,

    createOrder: (
        domainName: string,
        isRestartSubscription: boolean,
        paypalCurrency: string,
        serverApiService: ServerApiService,
        data: any,
        actions: any ) => {

        // Indicate order
        console.log( 'Prod PayPal create order for domain', domainName, 'with restart', isRestartSubscription )

        // Create the order
        return serverApiService.createInvoice( domainName, isRestartSubscription ).toPromise().then(( createdInvoice: CreatedInvoice ) => {

            // Indicate create order
            console.log( 'Prod PayPal created order', JSON.stringify( createdInvoice, null, 2 ) )

            // Return the order id
            return createdInvoice.orderId
        } )
    },
    capturePayment: (
        orderId: string,
        serverApiService: ServerApiService,
        data: any,
        actions: any ) => {

        // Indicate order
        console.log( 'Order: ' + orderId + " with data:\n\n" + JSON.stringify( data, null, 2 ) + "\n\n" );

        // Capture the payment
        return serverApiService.capturePayment( orderId ).toPromise().then(( domainPayments: DomainPayments ) => {

            // Indicate captured payment
            console.log( 'Prod PayPal captured payment', JSON.stringify( domainPayments, null, 2 ) )

            // Return captured details
            return domainPayments
        } )
    }

}