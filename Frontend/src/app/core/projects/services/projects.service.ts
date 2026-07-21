import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { inject } from '@angular/core';
import { Observable } from 'rxjs';
import Project from '../models/projects.model';
@Injectable({
  providedIn: 'root',
})
export class ProjectsService {
  private http = inject(HttpClient);
  private apiUrl = '/api/projects';

  loadProjects(): Observable<Project[]> {

    return this.http.get<Project[]>(this.apiUrl);
  }

  create(project: Partial<Project>): Observable<Project> {
    return this.http.post<Project>(this.apiUrl, project);
  }

  update(project: Partial<Project>): Observable<Project> {
    const response = this.http.put<Project>(`${this.apiUrl}/${project.id}`, project);
    return response;
  }

  delete(appId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${appId}`);
  }
}
