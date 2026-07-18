import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { inject,signal } from '@angular/core';
import Application from '../../interfaces/interfaces';
import { Observable } from 'rxjs';
@Injectable({
  providedIn: 'root',
})
export class ApplicationService {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8080/api/apps';

  loadApplications():Observable<Application[]> {
    return this.http.get<Application[]>(this.apiUrl);
  }

  create(app:Partial<Application>):Observable<Application> {
    return this.http.post<Application>(this.apiUrl, app);
  }

  update(app:Partial<Application>):Observable<Application> {
    return this.http.put<Application>(`${this.apiUrl}/${app.id}`, app);
  }

  delete(appId:number):Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${appId}`);
  }
}
