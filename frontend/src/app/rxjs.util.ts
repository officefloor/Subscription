import { Observable, OperatorFunction } from 'rxjs'
import { concatMap } from 'rxjs/operators'

// Fixes the typing for Typescript editor
export function concatFMap<T, O>( project: ( value: T, index: number ) => Observable<O> ): OperatorFunction<T, O> {
    return concatMap( project ) as any
}
