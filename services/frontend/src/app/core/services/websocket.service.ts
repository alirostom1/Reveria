import { Injectable, OnDestroy } from '@angular/core';
import { Client, IMessage } from '@stomp/stompjs';
import { environment } from '../../environments/environment';
import { Subject } from 'rxjs';

export interface StreamStatusEvent {
  streamUuid: string;
  channelUuid: string;
  status: 'IDLE' | 'LIVE' | 'ENDED';
  hlsPlaybackUrl?: string;
}

export interface ChatMessage {
  streamUuid: string;
  userUuid: string;
  displayName: string;
  avatarUrl?: string;
  content: string;
  timestamp: string;
}

export interface VideoProgressEvent {
  videoUuid: string;
  status: 'PROCESSING' | 'READY' | 'FAILED';
  step: string;
  progress: number;
  message: string;
}

@Injectable({ providedIn: 'root' })
export class WebSocketService implements OnDestroy {
  private client: Client;
  private subscriptions = new Map<string, any>();
  private streamSubjects = new Map<string, Subject<StreamStatusEvent>>();
  private chatSubjects = new Map<string, Subject<ChatMessage>>();
  private videoSubjects = new Map<string, Subject<VideoProgressEvent>>();

  constructor() {
    const wsBase = environment.wsUrl.replace(/^http/, 'ws');
    this.client = new Client({
      brokerURL: `${wsBase}/ws/websocket`,
      reconnectDelay: 5000,
      onConnect: () => {
        for (const topic of this.streamSubjects.keys()) {
          this.doSubscribe(topic, 'stream');
        }
        for (const topic of this.chatSubjects.keys()) {
          this.doSubscribe(topic, 'chat');
        }
        for (const topic of this.videoSubjects.keys()) {
          this.doSubscribe(topic, 'video');
        }
      },
      onDisconnect: () => {
        this.subscriptions.clear();
      },
    });
  }

  private ensureActive(): void {
    if (!this.client.active) {
      this.client.activate();
    }
  }

  // ── Stream status ──

  subscribeToStream(streamUuid: string): Subject<StreamStatusEvent> {
    const topic = `/topic/streams/${streamUuid}`;
    let subject = this.streamSubjects.get(topic);
    if (!subject) {
      subject = new Subject<StreamStatusEvent>();
      this.streamSubjects.set(topic, subject);
    }
    this.ensureActive();
    if (this.client.connected) this.doSubscribe(topic, 'stream');
    return subject;
  }

  unsubscribeFromStream(streamUuid: string): void {
    const topic = `/topic/streams/${streamUuid}`;
    this.removeSub(topic, this.streamSubjects);
  }

  // ── Chat ──

  subscribeToChat(streamUuid: string): Subject<ChatMessage> {
    const topic = `/topic/chat/${streamUuid}`;
    let subject = this.chatSubjects.get(topic);
    if (!subject) {
      subject = new Subject<ChatMessage>();
      this.chatSubjects.set(topic, subject);
    }
    this.ensureActive();
    if (this.client.connected) this.doSubscribe(topic, 'chat');
    return subject;
  }

  sendChatMessage(streamUuid: string, message: Omit<ChatMessage, 'streamUuid' | 'timestamp'>): void {
    if (!this.client.connected) return;
    this.client.publish({
      destination: `/app/chat/${streamUuid}`,
      body: JSON.stringify(message),
    });
  }

  unsubscribeFromChat(streamUuid: string): void {
    const topic = `/topic/chat/${streamUuid}`;
    this.removeSub(topic, this.chatSubjects);
  }

  // ── Video progress ──

  subscribeToVideoProgress(videoUuid: string): Subject<VideoProgressEvent> {
    const topic = `/topic/videos/${videoUuid}`;
    let subject = this.videoSubjects.get(topic);
    if (!subject) {
      subject = new Subject<VideoProgressEvent>();
      this.videoSubjects.set(topic, subject);
    }
    this.ensureActive();
    if (this.client.connected) this.doSubscribe(topic, 'video');
    return subject;
  }

  unsubscribeFromVideoProgress(videoUuid: string): void {
    const topic = `/topic/videos/${videoUuid}`;
    this.removeSub(topic, this.videoSubjects);
  }

  // ── Internal ──

  private doSubscribe(topic: string, type: 'stream' | 'chat' | 'video'): void {
    if (this.subscriptions.has(topic) || !this.client.connected) return;
    const subjectMap = type === 'stream' ? this.streamSubjects : type === 'chat' ? this.chatSubjects : this.videoSubjects;
    const subject = subjectMap.get(topic);
    if (!subject) return;

    const sub = this.client.subscribe(topic, (message: IMessage) => {
      subject.next(JSON.parse(message.body));
    });
    this.subscriptions.set(topic, sub);
  }

  private removeSub(topic: string, subjectMap: Map<string, Subject<any>>): void {
    const sub = this.subscriptions.get(topic);
    if (sub) {
      sub.unsubscribe();
      this.subscriptions.delete(topic);
    }
    const subject = subjectMap.get(topic);
    if (subject) {
      subject.complete();
      subjectMap.delete(topic);
    }
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach((sub) => sub.unsubscribe());
    this.subscriptions.clear();
    this.streamSubjects.forEach((s) => s.complete());
    this.streamSubjects.clear();
    this.chatSubjects.forEach((s) => s.complete());
    this.chatSubjects.clear();
    this.videoSubjects.forEach((s) => s.complete());
    this.videoSubjects.clear();
    this.client.deactivate();
  }
}
