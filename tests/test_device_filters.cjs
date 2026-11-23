#!/usr/bin/env node
'use strict';

// Exercise the actual inline HTML JavaScript in a lightweight DOM mock, not a
// duplicated implementation of the filtering algorithm.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');

const html = fs.readFileSync(path.join(__dirname, '..', 'web', 'index.html'), 'utf8');
assert.match(html, /<label>Hersteller<select id="manufacturer"><\/select><\/label>/);
assert.match(html, /<label>Gerätetyp<select id="category"><\/select><\/label>/);

class Option {
  constructor(text, value) { this.text = text; this.value = String(value); this.disabled = false; }
}
class Element {
  constructor(name) {
    this.id = name;
    this.value = '';
    this.options = [];
    this.children = [];
    this.listeners = {};
    this.textContent = '';
    this.hidden = false;
  }
  replaceChildren(...items) {
    this.children = items;
    if (['manufacturer','category','device','kind'].includes(this.id)) {
      this.options = items;
    }
  }
  append(item) { this.children.push(item); }
  addEventListener(event, callback) { this.listeners[event] = callback; }
  trigger(event = 'change') { assert.equal(typeof this.listeners[event], 'function'); this.listeners[event](); }
}
const ids = ['manufacturer','category','device','device-count','search','kind','rows','range','sources','status','device-title','notes'];
const elements = Object.fromEntries(ids.map(key => [key, new Element(key)]));
elements.kind.options = [
  new Option('CC und CC-Paare','cc'),
  new Option('Alle Einträge','all'),
  new Option('NRPN','NRPN')
];
elements.kind.value = 'cc';
const document = {
  getElementById(id) { assert.ok(elements[id], 'Unknown element: ' + id); return elements[id]; },
  createElement(tag) { return new Element(tag); },
  createDocumentFragment() { return new Element('fragment'); }
};
const script = html.match(/<script>([\s\S]*?)<\/script>/);
assert.ok(script, 'Missing inline UI script');
vm.runInNewContext(script[1], { document, Option }, { timeout: 3000 });

const maker = elements.manufacturer;
const type = elements.category;
const device = elements.device;
const count = elements['device-count'];
const search = elements.search;
const status = elements.status;

const optionNames = input => input.options.map(o => o.text);
const pick = (input, value) => {
  assert.ok(input.options.some(o => o.value === value), 'Unavailable filter ' + input.id + ':' + value);
  input.value = value;
  input.trigger();
};
assert.equal(count.textContent, '17 von 17 Geräten auswählbar');
assert.equal(device.options.length, 17);
assert.ok(optionNames(type).includes('Synthesizer'));
assert.ok(optionNames(type).includes('Effekt'));
assert.ok(optionNames(type).includes('Groovebox'));
assert.ok(optionNames(type).includes('Sampler / Drumcomputer'));

pick(maker, 'Erica Synths');
assert.deepEqual(optionNames(device), ['Erica Synths Bassline DB-01', 'Erica Synths Xenodrive']);
assert.deepEqual(optionNames(type), ['Alle Gerätetypen','Synthesizer','Effekt']);
assert.equal(count.textContent, '2 von 17 Geräten auswählbar');
pick(type, 'effect');
assert.deepEqual(optionNames(device), ['Erica Synths Xenodrive']);
assert.equal(elements['device-title'].textContent, 'Erica Synths Xenodrive');
assert.equal(count.textContent, '1 von 17 Geräten auswählbar');

pick(type, '');
pick(maker, '');
pick(type, 'effect');
assert.deepEqual(optionNames(maker), ['Alle Hersteller','Erica Synths','Strymon']);
assert.deepEqual(optionNames(device), ['Erica Synths Xenodrive', 'Strymon BigSky (Original)']);
assert.equal(count.textContent, '2 von 17 Geräten auswählbar');

pick(type, '');
pick(maker, 'Roland');
assert.equal(device.options.length, 3);
assert.deepEqual(optionNames(type), ['Alle Gerätetypen','Synthesizer','Groovebox']);
pick(type, 'groovebox');
assert.deepEqual(optionNames(device), ['Roland MC-707']);

pick(maker, '');
pick(type, 'sampler');
assert.deepEqual(optionNames(device), ['Elektron Digitakt (Original)','Elektron Octatrack MKII']);
assert.deepEqual(optionNames(maker), ['Alle Hersteller','Elektron']);

// Combined device-filter selections must not break CC/NRPN row searching.
pick(type, '');
pick(maker, 'Novation');
const peak = device.options.find(o=>o.text==='Novation Peak');
assert.ok(peak);
pick(device, peak.value);
search.value = '29:61';
search.trigger('input');
assert.match(status.textContent, /^1 von /);
assert.equal(elements.rows.children[0].children.length, 1);
assert.equal(elements.rows.children[0].children[0].children[0].textContent, '29:61');
assert.equal(elements.rows.children[0].children[0].children[1].textContent, 'Filter Frequency');

pick(maker, '');
assert.equal(device.options.length, 17);
console.log('PASS: 17 devices, coupled manufacturer/type filtering, CC pair search');
