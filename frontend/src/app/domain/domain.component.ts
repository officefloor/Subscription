import { Component, OnInit } from '@angular/core'
import { Router, ActivatedRoute, ParamMap } from '@angular/router'
import { switchMap } from 'rxjs/operators'

@Component( {
    selector: 'app-domain',
    templateUrl: './domain.component.html',
    styleUrls: ['./domain.component.css']
} )
export class DomainComponent implements OnInit {

    domainName: string

    constructor(
        private route: ActivatedRoute,
        private router: Router,
    ) { }

    ngOnInit() {
        // Load the domain payments for domain
        this.domainName = this.route.snapshot.paramMap.get( 'domain' )
        console.log( 'TOOD REMOVE domain: ', this.domainName )
    }

}
