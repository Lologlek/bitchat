import Foundation
import CoreBluetooth

protocol MeshServiceDelegate: AnyObject {
    func meshService(_ service: MeshService, didReceive packet: BitchatPacket, from peerID: String)
}

class MeshService: NSObject {
    static let serviceUUID = CBUUID(string: "F47B5E2D-4A9E-4C5A-9B3F-8E1D2C3A4B5C")
    static let characteristicUUID = CBUUID(string: "A1B2C3D4-E5F6-4A5B-8C9D-0E1F2A3B4C5D")

    private var centralManager: CBCentralManager!
    private var peripheralManager: CBPeripheralManager!
    private var discovered: [CBPeripheral] = []
    private var connected: [CBPeripheral] = []
    private var characteristic: CBCharacteristic?
    private var ownCharacteristic: CBMutableCharacteristic?

    weak var delegate: MeshServiceDelegate?

    override init() {
        super.init()
        centralManager = CBCentralManager(delegate: self, queue: nil)
        peripheralManager = CBPeripheralManager(delegate: self, queue: nil)
    }

    func start() {
        if centralManager.state == .poweredOn {
            centralManager.scanForPeripherals(withServices: [Self.serviceUUID], options: [CBCentralManagerScanOptionAllowDuplicatesKey: false])
        }
        if peripheralManager.state == .poweredOn {
            setupPeripheral()
            peripheralManager.startAdvertising([
                CBAdvertisementDataServiceUUIDsKey: [Self.serviceUUID],
                CBAdvertisementDataLocalNameKey: UUID().uuidString.prefix(8).description
            ])
        }
    }

    func stop() {
        centralManager.stopScan()
        peripheralManager.stopAdvertising()
        for peripheral in connected {
            centralManager.cancelPeripheralConnection(peripheral)
        }
    }

    func sendPacket(_ packet: BitchatPacket) {
        guard let data = packet.toBinaryData() else { return }
        for peripheral in connected {
            if let char = characteristic {
                peripheral.writeValue(data, for: char, type: .withoutResponse)
            }
        }
        if let char = ownCharacteristic {
            peripheralManager.updateValue(data, for: char, onSubscribedCentrals: nil)
        }
    }

    private func setupPeripheral() {
        let char = CBMutableCharacteristic(type: Self.characteristicUUID,
                                            properties: [.writeWithoutResponse, .notify],
                                            value: nil,
                                            permissions: [.writeable])
        let service = CBMutableService(type: Self.serviceUUID, primary: true)
        service.characteristics = [char]
        peripheralManager.add(service)
        ownCharacteristic = char
    }
}

extension MeshService: CBCentralManagerDelegate {
    func centralManagerDidUpdateState(_ central: CBCentralManager) {
        if central.state == .poweredOn {
            central.scanForPeripherals(withServices: [Self.serviceUUID], options: [CBCentralManagerScanOptionAllowDuplicatesKey: false])
        }
    }

    func centralManager(_ central: CBCentralManager, didDiscover peripheral: CBPeripheral, advertisementData: [String : Any], rssi RSSI: NSNumber) {
        if !discovered.contains(peripheral) {
            discovered.append(peripheral)
            peripheral.delegate = self
            central.connect(peripheral, options: nil)
        }
    }

    func centralManager(_ central: CBCentralManager, didConnect peripheral: CBPeripheral) {
        connected.append(peripheral)
        peripheral.discoverServices([Self.serviceUUID])
    }

    func centralManager(_ central: CBCentralManager, didDisconnectPeripheral peripheral: CBPeripheral, error: Error?) {
        connected.removeAll { $0 == peripheral }
    }
}

extension MeshService: CBPeripheralDelegate {
    func peripheral(_ peripheral: CBPeripheral, didDiscoverServices error: Error?) {
        guard let services = peripheral.services else { return }
        for service in services {
            peripheral.discoverCharacteristics([Self.characteristicUUID], for: service)
        }
    }

    func peripheral(_ peripheral: CBPeripheral, didDiscoverCharacteristicsFor service: CBService, error: Error?) {
        guard let characteristics = service.characteristics else { return }
        for char in characteristics where char.uuid == Self.characteristicUUID {
            characteristic = char
            peripheral.setNotifyValue(true, for: char)
        }
    }

    func peripheral(_ peripheral: CBPeripheral, didUpdateValueFor characteristic: CBCharacteristic, error: Error?) {
        guard characteristic.uuid == Self.characteristicUUID,
              let data = characteristic.value,
              let packet = BitchatPacket.from(data) else { return }
        let peerID = peripheral.identifier.uuidString
        delegate?.meshService(self, didReceive: packet, from: peerID)
    }
}

extension MeshService: CBPeripheralManagerDelegate {
    func peripheralManagerDidUpdateState(_ peripheral: CBPeripheralManager) {
        if peripheral.state == .poweredOn {
            setupPeripheral()
            peripheralManager.startAdvertising([
                CBAdvertisementDataServiceUUIDsKey: [Self.serviceUUID],
                CBAdvertisementDataLocalNameKey: UUID().uuidString.prefix(8).description
            ])
        }
    }

    func peripheralManager(_ peripheral: CBPeripheralManager, central: CBCentral, didSubscribeTo characteristic: CBCharacteristic) {
        // store for potential writes if needed
    }

    func peripheralManager(_ peripheral: CBPeripheralManager, didReceiveWrite requests: [CBATTRequest]) {
        for request in requests where request.characteristic.uuid == Self.characteristicUUID {
            if let value = request.value, let packet = BitchatPacket.from(value) {
                let peerID = request.central.identifier.uuidString
                delegate?.meshService(self, didReceive: packet, from: peerID)
            }
            peripheral.respond(to: request, withResult: .success)
        }
    }
}
