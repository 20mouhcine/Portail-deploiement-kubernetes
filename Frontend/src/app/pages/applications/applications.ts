import { Component, inject, OnInit } from '@angular/core';
        import { ApplicationService } from './application.service';
import Application from '../../interfaces/interfaces';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-apps',
  standalone: true,
  templateUrl: './applications.html',
  styleUrl: './applications.css',
    imports: [CommonModule, FormsModule],

})
export class Applications implements OnInit {
  private service = inject(ApplicationService);

  applications: Application[] = [];
  showForm = false;
  editingId: string | null = null;

  form = { name: '', description: '', port: 8080, replicas: 1 };

  ngOnInit() {
    this.refresh();
  }

  refresh() {
    this.service.loadApplications().subscribe(apps => this.applications = apps);
  }

  openCreateForm() {
    this.editingId = null;
    this.form = { name: '', description: '', port: 8080, replicas: 1 };
    this.showForm = true;
  }

  // openEditForm(app: Application) {
  //   this.editingId = app.id;
  //   this.form = { name: app.name, description: app.description, port: app.port, replicas: app.replicas };
  //   this.showForm = true;
  // }

  // save() {
  //   const request = this.editingId
  //     ? this.service.update(this.editingId, this.form)
  //     : this.service.create(this.form);

  //   request.subscribe(() => {
  //     this.showForm = false;
  //     this.refresh();
  //   });
  // }

  cancel() {
    this.showForm = false;
  }

  deleteApp(app: Application) {
    if (confirm(`Supprimer ${app.name} ?`)) {
      this.service.delete(app.id).subscribe(() => this.refresh());
    }
  }
}