import { ServerApiService } from '../app/server-api.service'

export const environment = {
    production: true,

    serverUrl: '',

    createOrder: (
        domainName: string,
        isRestartSubscription: boolean,
        paypalCurrency: string,
        serverApiService: ServerApiService,
        data: any,
        actions: any ) => {

        // Indicate order
        console.log( 'PayPal production environment create order for domain', domainName, 'with restart', isRestartSubscription )

        // TODO call server
        throw new Error( "TODO call server to create order" )
    },
    capturePayment: (
        orderId: string,
        serverApiService: ServerApiService,
        data: any,
        actions: any ) => {

        // Indicate order
        console.log( 'Order: ' + orderId + " with data:\n\n" + JSON.stringify( data, null, 2 ) + "\n\n" );

        // TODO call server
        throw new Error( "TODO call server to capture payment" )
    }

}