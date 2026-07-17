import { Component, inject, signal } from '@angular/core';
import { Router, RouterOutlet } from '@angular/router';
@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  private readonly router = inject(Router);
  protected readonly title = signal('Frontend');

  protected isHomePage(): boolean {
    return this.router.url === '/';
  }
}
